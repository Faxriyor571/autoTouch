package app;

import core.ClickService;
import core.CoordinateService;
import core.TimerService;
import ui.MainWindow;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CoordinateService coordinateService = new CoordinateService();
            TimerService      timerService      = new TimerService();
            ClickService      clickService      = new ClickService();

            new MainWindow(coordinateService, timerService, clickService);
        });
    }
}
