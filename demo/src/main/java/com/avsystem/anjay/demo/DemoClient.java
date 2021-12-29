/*
 * Copyright 2020-2021 AVSystem <avsystem@avsystem.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.avsystem.anjay.demo;

import com.avsystem.anjay.Anjay;
import com.avsystem.anjay.Anjay3dIpsoSensor;
import com.avsystem.anjay.AnjayAccessControl;
import com.avsystem.anjay.AnjayAccessControl.AccessMask;
import com.avsystem.anjay.AnjayAttrStorage;
import com.avsystem.anjay.AnjayAttributes;
import com.avsystem.anjay.AnjayBasicIpsoSensor;
import com.avsystem.anjay.AnjayEventLoop;
import com.avsystem.anjay.AnjayFirmwareUpdate;
import com.avsystem.anjay.AnjayFirmwareUpdate.InitialState;
import com.avsystem.anjay.AnjayFirmwareUpdate.Result;
import com.avsystem.anjay.AnjayFirmwareUpdateException;
import com.avsystem.anjay.AnjayFirmwareUpdateHandlers;
import com.avsystem.anjay.AnjayIpsoButton;
import com.avsystem.anjay.AnjaySecurityConfig;
import com.avsystem.anjay.AnjaySecurityInfoCert;
import com.avsystem.anjay.AnjaySecurityObject;
import com.avsystem.anjay.AnjayServerObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.time.Instant;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DemoClient implements Runnable {
    private Anjay.Configuration config;
    private AnjaySecurityObject securityObject;
    private AnjayServerObject serverObject;
    private AnjayAttrStorage attrStorage;
    private AnjayAccessControl accessControl;
    private DemoCommands demoCommands;
    public DemoArgs args;

    private AnjayIpsoButton button;

    public void pressButton() {
        button.update(0, true);
    }

    public void releaseButton() {
        button.update(0, false);
    }

    class FirmwareUpdateHandlers implements AnjayFirmwareUpdateHandlers {
        private File file;
        private FileOutputStream stream;
        private AnjayFirmwareUpdate firmwareUpdate;

        public void streamOpen(Optional<String> packageUri, Optional<byte[]> etag)
                throws Exception {
            this.file = new File("downloaded_firmware");
            this.stream = new FileOutputStream(file, false);
        }

        public void streamWrite(byte[] data) throws AnjayFirmwareUpdateException {
            try {
                this.stream.write(data);
            } catch (Exception e) {
                throw new AnjayFirmwareUpdateException(
                        AnjayFirmwareUpdateException.NOT_ENOUGH_SPACE, null);
            }
        }

        public void streamFinish() throws AnjayFirmwareUpdateException {
            try {
                this.stream.close();
            } catch (Exception e) {
                throw new AnjayFirmwareUpdateException(
                        AnjayFirmwareUpdateException.INTEGRITY_FAILURE, null);
            }
        }

        public void reset() {
            try {
                this.stream.close();
            } catch (Exception e) {
                Logger.getAnonymousLogger().log(Level.SEVERE, "failed to close stream: ", e);
            }
        }

        public String getName() {
            return "name";
        }

        public String getVersion() {
            return "1.0";
        }

        public void performUpgrade() throws AnjayFirmwareUpdateException {
            try {
                this.firmwareUpdate.setResult(Result.SUCCESS);
            } catch (Exception e) {
                throw new AnjayFirmwareUpdateException(
                        AnjayFirmwareUpdateException.INTEGRITY_FAILURE, null);
            }
        }

        public AnjaySecurityConfig getSecurityConfig(String uri) {
            if (DemoClient.this.args.fwCertFile == null) {
                return null;
            }

            AnjaySecurityInfoCert info = new AnjaySecurityInfoCert();
            info.serverCertValidation = true;
            info.trustedCerts = new LinkedList<>();

            try {
                CertificateFactory factory = CertificateFactory.getInstance("X.509");
                try (InputStream is = new FileInputStream(DemoClient.this.args.fwCertFile)) {
                    for (Certificate cert : factory.generateCertificates(is)) {
                        info.trustedCerts.add(
                                new AnjaySecurityInfoCert.Certificate(cert.getEncoded()));
                    }
                }
            } catch (CertificateException | IOException e) {
                throw new RuntimeException(e);
            }
            return new AnjaySecurityConfig(info);
        }

        public void setFirmwareUpdateObject(AnjayFirmwareUpdate firmwareUpdate) {
            this.firmwareUpdate = firmwareUpdate;
        }
    }

    public DemoClient(DemoArgs commandlineArgs) {
        this.args = commandlineArgs;
        this.config = new Anjay.Configuration();
        this.config.endpointName = this.args.endpointName;
        this.config.inBufferSize = 4000;
        this.config.outBufferSize = 4000;
        this.config.msgCacheSize = 4000;
        this.config.disableLegacyServerInitiatedBootstrap = this.args.bootstrapClientInitiatedOnly;
        this.config.udpTxParams =
                Optional.of(
                        new Anjay.CoapUdpTxParams(
                                this.args.ackTimeout,
                                this.args.ackRandomFactor,
                                this.args.maxRetransmit,
                                this.args.nstart));
        this.config.msgCacheSize = this.args.cacheSize;
    }

    private Optional<byte[]> readFile(String path) throws IOException {
        return Optional.of(Files.readAllBytes(Paths.get(path)));
    }

    public void configureDefaultServer() throws Exception {
        this.securityObject.purge();
        this.serverObject.purge();
        AnjaySecurityObject.Instance securityInstance = new AnjaySecurityObject.Instance();
        securityInstance.ssid = 1;
        securityInstance.serverUri = Optional.of(this.args.serverUri);

        if (this.args.securityMode == AnjaySecurityObject.SecurityMode.PSK) {
            securityInstance.publicCertOrPskIdentity =
                    Optional.ofNullable(this.args.identityOrCert);
            securityInstance.privateCertOrPskKey = Optional.ofNullable(this.args.pskOrPrivKey);
        } else if (this.args.securityMode == AnjaySecurityObject.SecurityMode.CERTIFICATE) {
            if (this.args.clientCertFile != null) {
                securityInstance.publicCertOrPskIdentity = readFile(this.args.clientCertFile);
                securityInstance.privateCertOrPskKey = readFile(this.args.keyFile);
            } else {
                securityInstance.publicCertOrPskIdentity =
                        Optional.ofNullable(this.args.identityOrCert);
                securityInstance.privateCertOrPskKey = Optional.ofNullable(this.args.pskOrPrivKey);
            }
        } else if (this.args.securityMode != AnjaySecurityObject.SecurityMode.NOSEC) {
            throw new RuntimeException("Unsupported security mode " + this.args.securityMode);
        }
        securityInstance.securityMode = this.args.securityMode;

        if (this.args.bootstrapClientInitiatedOnly) {
            this.args.bootstrap = true;
        }
        securityInstance.bootstrapServer = this.args.bootstrap;
        if (securityInstance.bootstrapServer) {
            securityInstance.clientHoldoffS = Optional.of(this.args.bootstrapHoldoff);
            securityInstance.bootstrapTimeoutS = Optional.of(this.args.bootstrapTimeout);
        }
        this.securityObject.addInstance(securityInstance);

        if (!securityInstance.bootstrapServer) {
            AnjayServerObject.Instance serverInstance = new AnjayServerObject.Instance();
            serverInstance.ssid = 1;
            serverInstance.lifetime = this.args.lifetime;
            serverInstance.binding = "U";
            int iid = this.serverObject.addInstance(serverInstance);
            this.accessControl.setAcl(
                    1,
                    iid,
                    serverInstance.ssid,
                    AccessMask.READ | AccessMask.WRITE | AccessMask.EXECUTE);
        }
    }

    private void maybeRestoreState() throws Exception {
        if (this.args.dmPersistenceFile != null) {
            try (FileInputStream restoreStream = new FileInputStream(this.args.dmPersistenceFile)) {
                this.securityObject.restore(restoreStream);
                this.serverObject.restore(restoreStream);
                this.accessControl.restore(restoreStream);
            } catch (Exception e) {
                Logger.getAnonymousLogger()
                        .log(
                                Level.SEVERE,
                                "failed to restore some of Security, Server and Access Control objects: ",
                                e);
                throw e;
            }
        } else {
            this.configureDefaultServer();
        }

        if (this.args.attributeStoragePersistenceFile != null) {
            try (FileInputStream restoreStream =
                    new FileInputStream(this.args.attributeStoragePersistenceFile)) {
                this.attrStorage.restore(restoreStream);
            } catch (Exception e) {
                Logger.getAnonymousLogger()
                        .log(Level.SEVERE, "failed to restore Attribute storage: ", e);
                throw e;
            }
        }
    }

    private void maybePersistState() {
        if (this.args.dmPersistenceFile != null
                && (this.securityObject.isModified()
                        || this.serverObject.isModified()
                        || this.accessControl.isModified())) {
            try (FileOutputStream persistStream =
                    new FileOutputStream(this.args.dmPersistenceFile)) {
                this.securityObject.persist(persistStream);
                this.serverObject.persist(persistStream);
                this.accessControl.persist(persistStream);
            } catch (Exception e) {
                Logger.getAnonymousLogger()
                        .log(
                                Level.SEVERE,
                                "failed to persist some of Security, Server and Access Control objects: ",
                                e);
            }
        }
        if (this.args.attributeStoragePersistenceFile != null && this.attrStorage.isModified()) {
            try (FileOutputStream persistStream =
                    new FileOutputStream(this.args.attributeStoragePersistenceFile)) {
                this.attrStorage.persist(persistStream);
            } catch (Exception e) {
                Logger.getAnonymousLogger()
                        .log(Level.SEVERE, "failed to persist Attribute storage: ", e);
            }
        }
    }

    private class FakeAccelerometer {
        public final double MIN_ACCELERATION = -100.0;
        public final double MAX_ACCELERATION = 100.0;

        private Random rand = new Random();

        private Double nextValue() {
            return (MIN_ACCELERATION + rand.nextDouble() * (MAX_ACCELERATION - MIN_ACCELERATION));
        }

        private Double xVal = 0.0;
        private Double yVal = 0.0;
        private Double zVal = 0.0;

        public FakeAccelerometer() {
            for (int i = 0; i < 10; i++) {
                xVal += nextValue() * 0.1;
                yVal += nextValue() * 0.1;
                zVal += nextValue() * 0.1;
            }
        }

        public Double getXAcceleration() {
            xVal = xVal * 0.9 + nextValue() * 0.1;
            return xVal;
        }

        public Double getYAcceleration() {
            yVal = yVal * 0.9 + nextValue() * 0.1;
            return yVal;
        }

        public Double getZAcceleration() {
            zVal = zVal * 0.9 + nextValue() * 0.1;
            return zVal;
        }
    }

    @Override
    public void run() {
        try (Anjay anjay = new Anjay(this.config);
                AnjayEventLoop eventLoop = new AnjayEventLoop(anjay, 100L)) {
            Thread stdinThread =
                    new Thread(
                            () -> {
                                try (BufferedReader reader =
                                        new BufferedReader(new InputStreamReader(System.in))) {
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        demoCommands.schedule(eventLoop, line);
                                    }
                                } catch (IOException e) {
                                    Logger.getAnonymousLogger()
                                            .log(Level.WARNING, "failed to read from stdin: ", e);
                                } finally {
                                    eventLoop.interrupt();
                                }
                            });
            stdinThread.start();

            this.securityObject = AnjaySecurityObject.install(anjay);
            this.serverObject = AnjayServerObject.install(anjay);
            this.attrStorage = AnjayAttrStorage.install(anjay);
            this.accessControl = AnjayAccessControl.install(anjay);
            this.demoCommands = new DemoCommands(anjay, this, this.attrStorage, this.accessControl);
            DemoObject demoObject = new DemoObject();
            anjay.registerObject(demoObject);

            InitialState initialState = new InitialState();
            FirmwareUpdateHandlers fwuHandlers = new FirmwareUpdateHandlers();
            AnjayFirmwareUpdate firmwareUpdate =
                    AnjayFirmwareUpdate.install(anjay, fwuHandlers, initialState);
            fwuHandlers.setFirmwareUpdateObject(firmwareUpdate);

            try {
                this.maybeRestoreState();
            } catch (Exception e) {
                this.configureDefaultServer();

                this.attrStorage.purge();
                AnjayAttributes.ObjectInstanceAttrs attrs =
                        new AnjayAttributes.ObjectInstanceAttrs();
                attrs.maxPeriod = 5;
                attrStorage.setObjectAttrs(1, demoObject.oid(), attrs);
            }

            if (this.args.accessEntries != null) {
                for (DemoArgs.AccessEntry accessEntry : this.args.accessEntries) {
                    this.accessControl.setAcl(
                            accessEntry.oid,
                            accessEntry.iid,
                            accessEntry.ssid,
                            accessEntry.accessMask);
                }
            }

            Logger.getAnonymousLogger().log(Level.INFO, "*** DEMO STARTUP FINISHED ***");

            button = AnjayIpsoButton.install(anjay);
            button.instanceAdd(0, "Button1");

            final double minTemp = 20.0;
            final double maxTemp = 40.0;
            AnjayBasicIpsoSensor thermometer = AnjayBasicIpsoSensor.install(anjay, 3303);
            thermometer.instanceAdd(
                    0,
                    "Cel",
                    Optional.of(20.0),
                    Optional.of(40.0),
                    new Supplier<>() {
                        Random rand = new Random();

                        @Override
                        public Double get() {
                            return minTemp + rand.nextDouble() * (maxTemp - minTemp);
                        }
                    });
            FakeAccelerometer accelerometer = new FakeAccelerometer();
            Anjay3dIpsoSensor accelerometerObject = Anjay3dIpsoSensor.install(anjay, 3313);
            accelerometerObject.instanceAdd(
                    0,
                    "m/s2",
                    Optional.of(accelerometer.MIN_ACCELERATION),
                    Optional.of(accelerometer.MAX_ACCELERATION),
                    new Supplier<Anjay3dIpsoSensor.Coordinates>() {
                        @Override
                        public Anjay3dIpsoSensor.Coordinates get() {
                            return new Anjay3dIpsoSensor.Coordinates(
                                    accelerometer.getXAcceleration(),
                                    accelerometer.getYAcceleration(),
                                    accelerometer.getZAcceleration());
                        }
                    });

            eventLoop.scheduleTask(
                    new Consumer<>() {
                        @Override
                        public void accept(AnjayEventLoop eventLoop) {
                            maybePersistState();
                            thermometer.update(0);
                            accelerometerObject.update(0);
                            eventLoop.scheduleTask(this, Instant.now().plusMillis(2000L));
                        }
                    },
                    Instant.now().plusMillis(2000L));

            try {
                eventLoop.run();
            } catch (IOException e) {
                stdinThread.interrupt();
            } finally {
                stdinThread.join();
            }
        } catch (Throwable t) {
            System.out.println("Unhandled exception happened during main loop: " + t);
            t.printStackTrace();
        }
    }
}
