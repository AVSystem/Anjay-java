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
import com.avsystem.anjay.Anjay.SocketEntry;
import com.avsystem.anjay.AnjayAccessControl;
import com.avsystem.anjay.AnjayAttrStorage;
import com.avsystem.anjay.AnjayAttributes.ObjectInstanceAttrs;
import com.avsystem.anjay.AnjayAttributes.ResourceAttrs;
import com.avsystem.anjay.AnjayDownload;
import com.avsystem.anjay.AnjayDownloadHandlers;
import com.avsystem.anjay.AnjaySecurityConfig;
import com.avsystem.anjay.AnjaySecurityInfoPsk;
import java.io.File;
import java.io.FileOutputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DemoCommands {
    private Anjay anjay;
    private DemoClient demoClient;
    private AnjayAttrStorage attrStorage;
    private AnjayAccessControl accessControl;
    private LinkedBlockingQueue<String> commands;
    private Map<String, DemoCommand> registeredCommands;

    interface DemoCommand {
        public void apply(String[] args) throws Exception;
    }

    class SendUpdateCmd implements DemoCommand {
        @Override
        public void apply(String[] args) throws Exception {
            int ssid = args.length == 2 ? Integer.parseInt(args[1]) : Anjay.SSID_ANY;
            if (ssid < 0 || ssid > 65535) {
                Logger.getAnonymousLogger().log(Level.SEVERE, "invalid SSID: " + args[1]);
                return;
            }
            try {
                DemoCommands.this.anjay.scheduleRegistrationUpdate(ssid);
                if (ssid == Anjay.SSID_ANY) {
                    Logger.getAnonymousLogger()
                            .log(Level.INFO, "registration update scheduled for all servers");
                } else {
                    Logger.getAnonymousLogger()
                            .log(Level.INFO, "registration update scheduled for server " + args[1]);
                }
            } catch (Exception e) {
                Logger.getAnonymousLogger()
                        .log(Level.SEVERE, "could not schedule registration update");
                return;
            }
        }
    }

    class ReconnectCmd implements DemoCommand {
        @Override
        public void apply(String[] args) throws Exception {
            try {
                Set<Anjay.Transport> transportSet = parseTransports(args);
                DemoCommands.this.anjay.scheduleReconnect(transportSet);
                Logger.getAnonymousLogger().log(Level.INFO, "reconnect scheduled");
            } catch (Exception e) {
                Logger.getAnonymousLogger().log(Level.SEVERE, "could not schedule reconnect");
            }
        }
    }

    class EnterOfflineCmd implements DemoCommand {
        @Override
        public void apply(String[] args) throws Exception {
            try {
                Set<Anjay.Transport> transportSet = parseTransports(args);
                DemoCommands.this.anjay.enterOffline(transportSet);
            } catch (Exception e) {
                Logger.getAnonymousLogger().log(Level.SEVERE, "could not enter offline");
            }
        }
    }

    class ExitOfflineCmd implements DemoCommand {
        @Override
        public void apply(String[] args) throws Exception {
            try {
                Set<Anjay.Transport> transportSet = parseTransports(args);
                DemoCommands.this.anjay.exitOffline(transportSet);
            } catch (Exception e) {
                Logger.getAnonymousLogger().log(Level.SEVERE, "could not exit offline");
            }
        }
    }

    class DownloadCmd implements DemoCommand {
        @Override
        public void apply(String[] args) throws Exception {
            if (args.length != 2 && args.length != 4) {
                Logger.getAnonymousLogger()
                        .log(
                                Level.SEVERE,
                                "unsupported format, must be \"download uri filename [identity psk]\"");
                return;
            }
            AnjayDownload.Configuration dlConfig = new AnjayDownload.Configuration();
            dlConfig.url = args[0];
            if (args.length == 4) {
                AnjaySecurityInfoPsk secInfo =
                        new AnjaySecurityInfoPsk(args[2].getBytes(), args[3].getBytes());
                dlConfig.securityConfig = Optional.of(new AnjaySecurityConfig(secInfo));
            }
            DownloadHandlers handlers = new DownloadHandlers(args[1]);
            AnjayDownload.startDownload(DemoCommands.this.anjay, dlConfig, handlers);
        }
    }

    class NonLwm2mSocketCountCmd implements DemoCommand {
        @Override
        public void apply(String[] args) throws Exception {
            List<SocketEntry> socketEntries = DemoCommands.this.anjay.getSocketEntries();
            int nonLwm2mSockets = 0;
            for (SocketEntry entry : socketEntries) {
                if (entry.ssid == Anjay.SSID_ANY) {
                    ++nonLwm2mSockets;
                }
            }
            System.out.println("NON_LWM2M_SOCKET_COUNT==" + nonLwm2mSockets);
        }
    }

    class GetTransportCmd implements DemoCommand {
        @Override
        public void apply(String[] args) throws Exception {
            if (args.length != 1) {
                Logger.getAnonymousLogger()
                        .log(Level.SEVERE, "unsupported format, must be \"get-transport index\"");
                return;
            }
            List<SocketEntry> socketEntries = DemoCommands.this.anjay.getSocketEntries();
            int index = Integer.parseInt(args[0]);
            if (index < 0) {
                index += socketEntries.size();
            }
            if (index < 0 || index >= socketEntries.size()) {
                Logger.getAnonymousLogger().log(Level.SEVERE, "index out of range");
            }
            System.out.println("TRANSPORT==" + socketEntries.get(index).transport);
        }
    }

    class DisableServerCmd implements DemoCommand {
        @Override
        public void apply(String[] args) throws Exception {
            if (args.length != 1) {
                Logger.getAnonymousLogger()
                        .log(Level.SEVERE, "unsupported format, must be \"disable-server ssid\"");
                return;
            }
            DemoCommands.this.anjay.disableServer(Integer.parseInt(args[0]));
        }
    }

    class EnableServerCmd implements DemoCommand {
        @Override
        public void apply(String[] args) throws Exception {
            if (args.length != 1) {
                Logger.getAnonymousLogger()
                        .log(Level.SEVERE, "unsupported format, must be \"enable-server ssid\"");
                return;
            }
            DemoCommands.this.anjay.enableServer(Integer.parseInt(args[0]));
        }
    }

    class DisableServerWithTimeoutCmd implements DemoCommand {
        @Override
        public void apply(String[] args) throws Exception {
            if (args.length != 2) {
                Logger.getAnonymousLogger()
                        .log(
                                Level.SEVERE,
                                "unsupported format, must be \"disable-server-with-timeout ssid timeout-seconds (with timeout-seconds < 0 meaning infinity)\"");
                return;
            }
            final int ssid = Integer.parseInt(args[0]);
            final int timeoutSeconds = Integer.parseInt(args[1]);
            DemoCommands.this.anjay.disableServerWithTimeout(
                    ssid,
                    timeoutSeconds < 0
                            ? Optional.empty()
                            : Optional.of(Duration.ofSeconds(timeoutSeconds)));
        }
    }

    class SetAttrsCmd implements DemoCommand {
        private ObjectInstanceAttrs getObjectInstanceAttrs(String[] args) throws Exception {
            ObjectInstanceAttrs attrs = new ObjectInstanceAttrs();

            for (String s : args) {
                String[] attr = s.split("=");
                if (attr.length != 2) {
                    throw new RuntimeException("invalid syntax, should be attr=value, is " + s);
                }

                int value = Integer.parseInt(attr[1]);

                switch (attr[0]) {
                    case "pmin":
                        attrs.minPeriod = value;
                        break;
                    case "pmax":
                        attrs.maxPeriod = value;
                        break;
                    case "epmin":
                        attrs.minEvalPeriod = value;
                        break;
                    case "epmax":
                        attrs.maxEvalPeriod = value;
                        break;
                    default:
                        throw new RuntimeException("unknown attribute " + attr[0]);
                }
            }

            return attrs;
        }

        private ResourceAttrs getResourceAttrs(String[] args) {
            ResourceAttrs attrs = new ResourceAttrs();

            for (String s : args) {
                String[] attr = s.split("=");
                if (attr.length != 2) {
                    throw new RuntimeException("invalid syntax, should be attr=value, is " + s);
                }

                switch (attr[0]) {
                    case "pmin":
                        attrs.common.minPeriod = Integer.parseInt(attr[1]);
                        break;
                    case "pmax":
                        attrs.common.maxPeriod = Integer.parseInt(attr[1]);
                        break;
                    case "epmin":
                        attrs.common.minEvalPeriod = Integer.parseInt(attr[1]);
                        break;
                    case "epmax":
                        attrs.common.maxEvalPeriod = Integer.parseInt(attr[1]);
                        break;
                    case "lt":
                        attrs.lessThan = Double.parseDouble(attr[1]);
                        break;
                    case "gt":
                        attrs.greaterThan = Double.parseDouble(attr[1]);
                        break;
                    case "st":
                        attrs.step = Double.parseDouble(attr[1]);
                        break;
                    default:
                        throw new RuntimeException("unknown attribute " + attr[0]);
                }
            }

            return attrs;
        }

        private int[] getPath(String path) {
            String[] splitString = path.split("/");

            // First element should be an empty string
            if (!splitString[0].isEmpty()) {
                throw new RuntimeException("invalid path format");
            }
            int pathLength = splitString.length - 1;

            if (pathLength < 1 || pathLength > 4) {
                throw new RuntimeException("invalid path length");
            }

            int[] result = new int[pathLength];

            for (int i = 0; i < pathLength; i++) {
                result[i] = Integer.parseInt(splitString[i + 1]);
            }

            return result;
        }

        @Override
        public void apply(String[] args) throws Exception {
            if (args.length < 2) {
                throw new RuntimeException(
                        "unsupported format, must be \"[/a [/b [/c [/d] ] ] ] ssid [pmin,pmax,lt,gt,st,epmin,epmax]\" - e.g. /a/b 1 pmin=3 pmax=4");
            }

            int[] path = getPath(args[0]);
            int ssid = Integer.parseInt(args[1]);
            String[] attrs = Arrays.copyOfRange(args, 2, args.length);

            switch (path.length) {
                case 1:
                    DemoCommands.this.attrStorage.setObjectAttrs(
                            ssid, path[0], getObjectInstanceAttrs(attrs));
                    break;
                case 2:
                    DemoCommands.this.attrStorage.setInstanceAttrs(
                            ssid, path[0], path[1], getObjectInstanceAttrs(attrs));
                    break;
                case 3:
                    DemoCommands.this.attrStorage.setResourceAttrs(
                            ssid, path[0], path[1], path[2], getResourceAttrs(attrs));
                    break;
                default:
                    throw new RuntimeException("path too long");
            }
        }
    }

    class GetPortCmd implements DemoCommand {
        @Override
        public void apply(String[] args) throws Exception {
            if (args.length != 1) {
                Logger.getAnonymousLogger()
                        .log(Level.SEVERE, "Invalid index: " + String.join(" ", args));
                return;
            }
            List<SocketEntry> socketEntries = anjay.getSocketEntries();
            int socketsLength = socketEntries.size();
            int index = Integer.parseInt(args[0]);
            if (index < 0) {
                index = socketsLength + index;
            }
            try {
                SocketEntry socketEntry = socketEntries.get(index);
                int port = socketEntry.port;
                System.out.println("PORT==" + port);
            } catch (IndexOutOfBoundsException e) {
                Logger.getAnonymousLogger()
                        .log(
                                Level.SEVERE,
                                "Index out of range: "
                                        + index
                                        + "; socketsLength = "
                                        + socketsLength);
            }
        }
    }

    class SetAclCmd implements DemoCommand {
        @Override
        public void apply(String[] args) throws Exception {
            if (args.length != 1) {
                Logger.getAnonymousLogger().log(Level.SEVERE, "Must provide acl");
                return;
            }
            DemoArgs.AccessEntry accessEntry = DemoArgs.convertAccessEntry(args[0]);
            if (accessEntry != null) {
                DemoCommands.this.accessControl.setAcl(
                        accessEntry.oid, accessEntry.iid, accessEntry.ssid, accessEntry.accessMask);
            } else {
                Logger.getAnonymousLogger()
                        .log(Level.SEVERE, "Could not set acl, format is: /OID/IID,SSID,mask");
            }
        }
    }

    class RemoveServerCmd implements DemoCommand {
        @Override
        public void apply(String[] args) throws Exception {
            if (args.length != 0) {
                Logger.getAnonymousLogger()
                        .log(Level.SEVERE, "unsupported format, must be \"remove-server\"");
                return;
            }

            DemoCommands.this.demoClient.args.serverUri = "";
            try {
                DemoCommands.this.demoClient.configureDefaultServer();
            } catch (Exception e) {
            }
        }
    }

    static class DownloadHandlers implements AnjayDownloadHandlers {
        private final File file;
        private final FileOutputStream stream;

        public DownloadHandlers(String filename) throws Exception {
            this.file = new File(filename);
            this.stream = new FileOutputStream(file, false);
        }

        public void onNextBlock(byte[] args, Optional<byte[]> etag) throws Exception {
            this.stream.write(args);
        }

        public void onDownloadFinished(
                AnjayDownload.Result result, Optional<AnjayDownload.ResultDetails> details) {
            try {
                this.stream.close();
            } catch (Exception e) {
                Logger.getAnonymousLogger().log(Level.SEVERE, "failed to close stream: ", e);
            }
        }
    }

    public DemoCommands(
            Anjay anjay,
            DemoClient client,
            AnjayAttrStorage attrStorage,
            AnjayAccessControl accessControl) {
        this.anjay = anjay;
        this.demoClient = client;
        this.attrStorage = attrStorage;
        this.accessControl = accessControl;
        this.commands = new LinkedBlockingQueue<String>();
        this.registeredCommands = new HashMap<>();

        registeredCommands.put("download", new DownloadCmd());
        registeredCommands.put(
                "socket-count",
                (args) -> {
                    System.out.println("SOCKET_COUNT==" + this.anjay.getSockets().size());
                });
        registeredCommands.put("non-lwm2m-socket-count", new NonLwm2mSocketCountCmd());
        registeredCommands.put("get-transport", new GetTransportCmd());
        registeredCommands.put("get-port", new GetPortCmd());
        registeredCommands.put("disable-server", new DisableServerCmd());
        registeredCommands.put("enable-server", new EnableServerCmd());
        registeredCommands.put("disable-server-with-timeout", new DisableServerWithTimeoutCmd());
        registeredCommands.put("send-update", new SendUpdateCmd());
        registeredCommands.put("reconnect", new ReconnectCmd());
        registeredCommands.put("set-attrs", new SetAttrsCmd());
        registeredCommands.put("set-acl", new SetAclCmd());
        registeredCommands.put("enter-offline", new EnterOfflineCmd());
        registeredCommands.put("exit-offline", new ExitOfflineCmd());
        registeredCommands.put("remove-server", new RemoveServerCmd());
    }

    private Set<Anjay.Transport> parseTransports(String[] args) throws Exception {
        Set<Anjay.Transport> transportSet = new HashSet<>();
        for (String arg : args) {
            switch (arg) {
                case "ip":
                    transportSet.add(Anjay.Transport.TCP);
                    transportSet.add(Anjay.Transport.UDP);
                    break;
                case "tcp":
                    transportSet.add(Anjay.Transport.TCP);
                    break;
                case "udp":
                    transportSet.add(Anjay.Transport.UDP);
                    break;
                default:
                    Logger.getAnonymousLogger().log(Level.SEVERE, "unrecognized transport " + arg);
                    throw new Exception();
            }
        }
        return transportSet;
    }

    private void executeCommand(String cmd) throws Exception {
        String[] args = cmd.split(" ");

        if (args.length == 0) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "no command provided");
            return;
        }

        DemoCommand command = registeredCommands.get(args[0]);
        if (command == null) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "unknown command: " + args[0]);
            return;
        }

        command.apply(Arrays.copyOfRange(args, 1, args.length));

        System.out.println("(DEMO)>");
    }

    public void executeAll() {
        String command;
        while ((command = this.commands.poll()) != null) {
            try {
                this.executeCommand(command);
            } catch (Exception e) {
                Logger.getAnonymousLogger()
                        .log(Level.SEVERE, String.format("failed to execute: %s, %s", command, e));
            }
        }
    }

    public void schedule(String command) {
        commands.add(command);
    }
}
