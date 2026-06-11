package edu.icesi.sitmmio.distributed;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;

import edu.icesi.sitmmio.cli.CliOptions;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Locale;

public final class IceScanWorkerServer {
    public int run(CliOptions options, PrintStream out) {
        Path workDirectory = options.workDirectory() == null
                ? Path.of("results", "ice-worker")
                : options.workDirectory();
        String endpoints = String.format(Locale.ROOT,
                "tcp -h %s -p %d",
                options.iceHost(),
                options.icePort());

        try (Communicator communicator = Util.initialize(new String[]{"--Ice.MessageSizeMax=0"})) {
            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints("SitmMioIceWorker", endpoints);
            adapter.add(new IceScanWorkerServant(workDirectory), Util.stringToIdentity(options.iceIdentity()));
            adapter.activate();
            out.printf(Locale.ROOT,
                    "SITM-MIO Ice worker listening%nIdentity: %s%nEndpoint: %s%nWork directory: %s%n",
                    options.iceIdentity(),
                    endpoints,
                    workDirectory);
            communicator.waitForShutdown();
            return 0;
        }
    }
}
