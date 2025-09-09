package io.github.nhtuan10.mykafkatool.ui.event;

public record ApplicationUIEvent(Action action) implements UIEvent {
    public static boolean isApplicationReadyEvent(ApplicationUIEvent event) {
        return event.action() == Action.APP_READY;
    }

    public static ApplicationUIEvent newApplicationReadyEvent() {
        return new ApplicationUIEvent(Action.APP_READY);
    }
}
