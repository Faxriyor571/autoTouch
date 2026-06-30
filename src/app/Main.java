package app;

import core.ClickService;
import core.CoordinateService;
import core.HotkeyService;
import core.TimerService;
import ui.MainWindow;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {

        CoordinateService coordinateService = new CoordinateService();
        TimerService      timerService      = new TimerService();
        ClickService      clickService      = new ClickService();
        HotkeyService     hotkeyService     = new HotkeyService();

        hotkeyService.start(() -> {
            System.out.println("[MAIN] addCurrentMousePosition() chaqirilmoqda");
            coordinateService.addCurrentMousePosition();
        });

        SwingUtilities.invokeLater(() ->
                new MainWindow(
                        coordinateService,
                        timerService,
                        clickService,
                        hotkeyService
                )
        );
    }
}
