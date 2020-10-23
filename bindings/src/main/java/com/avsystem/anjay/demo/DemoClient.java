/*
 * Copyright 2020 AVSystem <avsystem@avsystem.com>
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
import com.avsystem.anjay.AnjayAttrStorage;
import com.avsystem.anjay.AnjayAttributes;
import com.avsystem.anjay.AnjayFirmwareUpdate;
import com.avsystem.anjay.AnjayFirmwareUpdate.InitialState;
import com.avsystem.anjay.AnjayFirmwareUpdate.Result;
import com.avsystem.anjay.AnjayFirmwareUpdateException;
import com.avsystem.anjay.AnjayFirmwareUpdateHandlers;
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
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DemoClient implements Runnable {
    private Anjay.Configuration config;
    private DemoArgs args;
    private AnjaySecurityObject securityObject;
    private AnjayServerObject serverObject;
    private AnjayAttrStorage attrStorage;
    private DemoCommands demoCommands;

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
    }

    private void configureDefaultServer() throws Exception {
        this.securityObject.purge();
        this.serverObject.purge();
        AnjaySecurityObject.Instance securityInstance = new AnjaySecurityObject.Instance();
        securityInstance.ssid = 1;
        securityInstance.serverUri = Optional.of(this.args.serverUri);

        if (this.args.securityMode == AnjaySecurityObject.SecurityMode.PSK
                || this.args.securityMode == AnjaySecurityObject.SecurityMode.CERTIFICATE) {
            securityInstance.publicCertOrPskIdentity = Optional.of(this.args.identityOrCert);
            securityInstance.privateCertOrPskKey = Optional.of(this.args.pskOrPrivKey);
        } else if (this.args.securityMode != AnjaySecurityObject.SecurityMode.NOSEC) {
            throw new RuntimeException("Unsupported security mode " + this.args.securityMode);
        }
        securityInstance.securityMode = this.args.securityMode;

        this.securityObject.addInstance(securityInstance);

        AnjayServerObject.Instance serverInstance = new AnjayServerObject.Instance();
        serverInstance.ssid = 1;
        serverInstance.lifetime = this.args.lifetime;
        serverInstance.binding = "U";
        this.serverObject.addInstance(serverInstance);
    }

    private void maybeRestoreState() throws Exception {
        if (this.args.dmPersistenceFile != null) {
            try (FileInputStream restoreStream = new FileInputStream(this.args.dmPersistenceFile)) {
                this.securityObject.restore(restoreStream);
                this.serverObject.restore(restoreStream);
            } catch (Exception e) {
                Logger.getAnonymousLogger()
                        .log(Level.SEVERE, "failed to restore Security and/or Server object: ", e);
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
                && (this.securityObject.isModified() || this.serverObject.isModified())) {
            try (FileOutputStream persistStream =
                    new FileOutputStream(this.args.dmPersistenceFile)) {
                this.securityObject.persist(persistStream);
                this.serverObject.persist(persistStream);
            } catch (Exception e) {
                Logger.getAnonymousLogger()
                        .log(Level.SEVERE, "failed to persist Security and/or Server object: ", e);
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

    @Override
    public void run() {
        AtomicBoolean shouldTerminate = new AtomicBoolean(false);
        Thread stdinThread =
                new Thread(
                        () -> {
                            try (BufferedReader reader =
                                    new BufferedReader(new InputStreamReader(System.in))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    demoCommands.schedule(line);
                                }
                            } catch (IOException e) {
                                Logger.getAnonymousLogger()
                                        .log(Level.WARNING, "failed to read from stdin: ", e);
                            } finally {
                                shouldTerminate.set(true);
                            }
                        });
        stdinThread.start();

        try (Anjay anjay = new Anjay(this.config)) {
            this.securityObject = AnjaySecurityObject.install(anjay);
            this.serverObject = AnjayServerObject.install(anjay);
            this.attrStorage = AnjayAttrStorage.install(anjay);
            this.demoCommands = new DemoCommands(anjay);
            DemoObject demoObject = new DemoObject();
            anjay.registerObject(demoObject);

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

            InitialState initialState = new InitialState();
            FirmwareUpdateHandlers fwuHandlers = new FirmwareUpdateHandlers();
            AnjayFirmwareUpdate firmwareUpdate =
                    AnjayFirmwareUpdate.install(anjay, fwuHandlers, initialState);
            fwuHandlers.setFirmwareUpdateObject(firmwareUpdate);

            Logger.getAnonymousLogger().log(Level.INFO, "*** DEMO STARTUP FINISHED ***");

            try (Selector selector = Selector.open()) {
                final long maxWaitMs = 1000L;
                while (!shouldTerminate.get()) {
                    this.demoCommands.executeAll();
                    List<SelectableChannel> sockets = anjay.getSockets();

                    for (SelectionKey key : selector.keys()) {
                        if (!sockets.contains(key.channel())) {
                            key.cancel();
                        }
                    }
                    for (SelectableChannel socket : sockets) {
                        if (socket.keyFor(selector) == null) {
                            socket.register(selector, SelectionKey.OP_READ);
                        }
                    }
                    long waitTimeMs = anjay.timeToNext().map(Duration::toMillis).orElse(maxWaitMs);
                    if (waitTimeMs > maxWaitMs) {
                        waitTimeMs = maxWaitMs;
                    }
                    if (waitTimeMs <= 0) {
                        selector.selectNow();
                    } else {
                        selector.select(waitTimeMs);
                    }
                    for (Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                            it.hasNext(); ) {
                        anjay.serve(it.next().channel());
                        it.remove();
                    }
                    anjay.schedRun();

                    this.maybePersistState();
                }
            }
        } catch (Throwable t) {
            System.out.println("Unhandled exception happened during main loop: " + t);
            t.printStackTrace();
        } finally {
            try {
                stdinThread.join();
            } catch (InterruptedException e) {
                // that's unlikely to happen
            }
        }
    }
}
