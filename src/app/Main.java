package app;

import core.ClickService;
import core.CoordinateService;
import core.HotkeyService;
import core.TimerService;
import result.AdaptiveLatencyModel;
import result.ResultObserverService;
import time.UzexTimeSyncService;
import ui.MainWindow;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {

        CoordinateService coordinateService = new CoordinateService();
        TimerService      timerService      = new TimerService();
        ClickService      clickService      = new ClickService();
        HotkeyService     hotkeyService     = new HotkeyService();
        UzexTimeSyncService timeSyncService = new UzexTimeSyncService();
        ResultObserverService resultObserverService = new ResultObserverService();
        AdaptiveLatencyModel adaptiveLatencyModel = new AdaptiveLatencyModel();

        hotkeyService.start(() -> {
            System.out.println("[MAIN] addCurrentMousePosition() chaqirilmoqda");
            coordinateService.addCurrentMousePosition();
        });

        SwingUtilities.invokeLater(() ->
                new MainWindow(
                        coordinateService,
                        timerService,
                        clickService,
                        hotkeyService,
                        timeSyncService,
                        resultObserverService,
                        adaptiveLatencyModel
                )
        );
    }
}
