package ru.ratnikov.ShowBillBot.Util;

import com.vdurmont.emoji.EmojiParser;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ru.ratnikov.ShowBillBot.model.Event;
import ru.ratnikov.ShowBillBot.model.Person;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Michael Gosling
 */
public class BotUtil {
    public static final String HELP_MESSAGE = "Привет!\nС помощью этого бота можно" +
            " посмотреть список ближайших анонсированных мероприятий, узнать, где и когда они проходят," +
            " а также зарегистрироваться и пойти на интересующие лично тебя (если, конечно, есть свободные места!)" +
            "\nЧтобы начать работать с ботом, введи команду /start или напиши \"Поехали!\"" +
            "\n\nПросматривать афишу и регистрироваться можно на вкладке /events," +
            " командой /signed можно увидеть список мероприятий, на которые ты зарегистрирован(а)," +
            " если нужно отменить регистрацию, то напиши мне /cancel." +
            "\n\nЕсли мероприятие отменится - я обязательно тебе сообщу!\n" +
            "Также я предусмотрел возможность изменения регистрационных данных на вкладке /edit.";
    public static final String ALREADY_REGISTERED = EmojiParser.parseToUnicode("Ты уже зарегистрирован(а). :white_check_mark:\nСписок доступных команд можно посмотреть в меню," +
            " или отправь /help, чтобы получить справку.");
    public static final String BOT_REGISTRATION_SUCCESS = EmojiParser.parseToUnicode(", поздравляю с регистрацией! :blush:\nОткрой меню, чтобы увидеть список команд," +
            " или отправь /help, чтобы получить справку.");
    public static final String NEED_TO_REGISTER = "Сначала нужно зарегистрироваться. Отправь команду /start";
    public static final String UNKNOWN_COMMAND = EmojiParser.parseToUnicode("Извини, этого я не понимаю " + ":pensive:" + "\nВоспользуйся меню!");
    public static final String FOR_ALL_EVENT_LIST = "Список предстоящих мероприятий ниже. Для просмотра информации нажмите на интересующее событие.";
    public static final String FOR_PERSON_EVENT_LIST = "Список мероприятий, на которые вы зарегистрированы." +
            "\nДля отмены регистрации нажмите на мероприятие";
    public static final String FOR_NO_REG_FOR_EVENTS = EmojiParser.parseToUnicode("Ты еще не зарегистрировался(ась) ни на одной мероприятие.");
    public static final String BACK_TO_MENU = "Возврат в меню";
    public static final String EVENT_REGISTRATION_SUCCESS = "Вы зарегистрировались. Мероприятие отображено в Вашем списке";
    public static final String REGISTRATION_CANCELLED = "Регистрация на мероприятие отменена.";
    public static final String B_REG_YES = "REG_YES_BUTTON";
    public static final String B_REG_NO = "REG_NO_BUTTON";
    public static final String B_CANCEL_REG_YES = "CANCEL_REG_YES";
    public static final String B_CANCEL_REG_NO = "CANCEL_REG_NO";
    public static final String START_COMM = "/start";
    public static final String START_COMM_TEXT_1 = "Поехали";
    public static final String START_COMM_TEXT_2 = "Поехали!";
    public static final String EVENTS_COMM = "/events";
    public static final String EVENTS_COMM_TEXT = "Ближайшие мероприятия";
    public static final String HELP_COMM = "/help";
    public static final String HELP_COMM_TEXT = "Помощь";
    public static final String SIGNED_COMM = "/signed";
    public static final String SIGNED_COMM_TEXT = "Мои мероприятия";
    public static final String EDIT_COMM = "/edit";
    public static final String EDIT_COMM_TEXT = "Изменить данные";
    public static final String ADMIN_MODE_COMM = "/admin";
    public static final String ADMIN_MODE_COMM_TEXT = "Режим админа";
    public static final String NOT_READY = "Это пока не реализовано";

    public static SendMessage createMessageTemplate(Person person) {
        return createMessageTemplate(person.getChatId());
    }
    final static ReplyKeyboardMarkup keyboardMenuMarkup = new ReplyKeyboardMarkup();
    final static List<BotCommand> commandsList = new ArrayList<>();

    static {
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(EVENTS_COMM_TEXT);
        row.add(SIGNED_COMM_TEXT);
        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add(HELP_COMM_TEXT);
        row.add(EDIT_COMM_TEXT);
        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add(ADMIN_MODE_COMM_TEXT);
        keyboardRows.add(row);
        keyboardMenuMarkup.setKeyboard(keyboardRows);
        keyboardMenuMarkup.setResizeKeyboard(true);

        commandsList.add(new BotCommand(START_COMM, "Начать общение с ботом"));
        commandsList.add(new BotCommand(EVENTS_COMM, "Список доступных мероприятий"));
        commandsList.add(new BotCommand(SIGNED_COMM, "Моя афиша"));
        commandsList.add(new BotCommand(HELP_COMM, "Помощь"));
        commandsList.add(new BotCommand(EDIT_COMM, "Изменить данные"));
        commandsList.add(new BotCommand(ADMIN_MODE_COMM, "Режим админа"));
    }

    public static SendMessage createMessageTemplate(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.enableMarkdown(true);
        return message;
    }

    public static SendMessage createMessageTemplate(long chatId, String text) {
        SendMessage message = createMessageTemplate(chatId);
        message.setText(text);
        return message;
    }

    public static SendMessage createMessageTemplateWithMenu (Long chatId, String text) {
        SendMessage message = createMessageTemplate(chatId);
        message.setText(text);
        message.setReplyMarkup(keyboardMenuMarkup);

        return message;
    }

    public static EditMessageText createEditMessageTextTemplate (String text, long chatId, long messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId(Integer.parseInt(String.valueOf(messageId)));
        return message;
    }

    public static InlineKeyboardButton createInlineKeyboardButton(String text, String command) {
        InlineKeyboardButton ikb = new InlineKeyboardButton();
        ikb.setText(text);
        ikb.setCallbackData(command);
        return ikb;
    }

    public static List<BotCommand> getBotCommandsList() {
        return commandsList;
    }

    public static void prepareMessageWithEventsList(List<Event> events, SendMessage message) {
        InlineKeyboardMarkup inlineMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> inlineRows = new ArrayList<>();

        for (Event event : events) {
            List<InlineKeyboardButton> inlineRow = new ArrayList<>();
            var eventButton = BotUtil.createInlineKeyboardButton(event.getTitle(), String.valueOf(event.getEventId()));
            inlineRow.add(eventButton);
            inlineRows.add(inlineRow);
        }

        inlineMarkup.setKeyboard(inlineRows);
        message.setReplyMarkup(inlineMarkup);
    }
}
