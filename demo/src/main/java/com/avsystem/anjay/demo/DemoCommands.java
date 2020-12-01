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
import com.avsystem.anjay.Anjay.SocketEntry;
import com.avsystem.anjay.AnjayDownload;
import com.avsystem.anjay.AnjayDownloadHandlers;
import com.avsystem.anjay.AnjaySecurityConfig;
import com.avsystem.anjay.AnjaySecurityInfoPsk;
import java.io.File;
import java.io.FileOutputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DemoCommands {
    private Anjay anjay;
    private LinkedBlockingQueue<String> commands;
    private Map<String, DemoCommand> registeredCommands;

    interface DemoCommand {
        public void apply(String[] args) throws Exception;
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

    public DemoCommands(Anjay anjay) {
        this.anjay = anjay;
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
        registeredCommands.put("disable-server", new DisableServerCmd());
        registeredCommands.put("enable-server", new EnableServerCmd());
        registeredCommands.put("disable-server-with-timeout", new DisableServerWithTimeoutCmd());
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
