package fr.ema.dedal.componentinspector.main;

import java.lang.instrument.Instrumentation;
import java.util.UUID;

public class InstrumentHook {
	
	private InstrumentHook() {}

    public static void premain(String agentArgs, Instrumentation inst) {
        if (agentArgs != null) {
            System.getProperties().put(AGENT_ARGS_KEY, agentArgs);
        }
        System.getProperties().put(INSTRUMENTATION_KEY, inst);
    }

    public static Instrumentation getInstrumentation() {
        return (Instrumentation) System.getProperties().get(INSTRUMENTATION_KEY);
    }

    // Needn't be a UUID - can be a String or any other object that
    // implements equals().    
    private static final Object AGENT_ARGS_KEY =
        UUID.randomUUID();

    private static final Object INSTRUMENTATION_KEY =
        UUID.randomUUID();

}