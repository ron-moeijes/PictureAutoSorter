package nl.ronmoeijes;

import static nl.ronmoeijes.Constants.COLON;
import static nl.ronmoeijes.Constants.SPACE;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.fusesource.jansi.Ansi.ansi;

public class Message {
    private String action;
    private String source;
    private String link;
    private String target;

    Message (String action, Object source, String link, Object target) {
        this.action = action;
        this.source = source.toString();
        this.link = link;
        this.target = target.toString();
    }

    Message (String action, Object source) {
        this(action, source, EMPTY, EMPTY);
    }

    void print() {
        try {
            System.out.println(action.toUpperCase() + COLON + SPACE + source + SPACE + link + SPACE + target);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void color(String color) {
        try {
            String message = action.toUpperCase() + COLON + SPACE + source + SPACE + link + SPACE + target;
            System.out.println(ansi().eraseScreen().render("@|"+ color + SPACE + message + "|@"));
        } catch (IllegalArgumentException e) {
            this.color();
        }
    }

    void color() {
        try {
            String color = this.getMessageColor();
            String message = action.toUpperCase() + COLON + SPACE + source + SPACE + link + SPACE + target;
            System.out.println(ansi().eraseScreen().render("@|"+ color + SPACE + message + "|@"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getMessageColor() {
        String color;
        switch (action) {
            case "warn":
                color = "yellow";
                break;
            case "info: ":
                color = "blue";
                break;
            case "deleted":
                color = "red";
                break;
            case "created":
                color = "green";
                break;
            case "source":
            case "moved":
            default:
                return "white";
        } return color;

    }
//    static void infoMessage(String action, Object source, String link, Object target) {
//        try {
//            System.out.println(action.toUpperCase() + source + link + target);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    static void infoMessage(String action, Object target) {
//        try {
//            System.out.println(action.toUpperCase() + COLON + target);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
