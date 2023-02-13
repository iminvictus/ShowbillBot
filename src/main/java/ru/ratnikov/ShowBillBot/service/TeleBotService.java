package ru.ratnikov.ShowBillBot.service;

import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.ratnikov.ShowBillBot.config.BotConfig;
import ru.ratnikov.ShowBillBot.model.Event;
import ru.ratnikov.ShowBillBot.model.Person;
import ru.ratnikov.ShowBillBot.repository.EventsRepository;
import ru.ratnikov.ShowBillBot.repository.PeopleRepository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Michael Gosling
 */
@Slf4j
@Service
@Transactional
public class TeleBotService extends TelegramLongPollingBot {

    private final PeopleRepository peopleRepository;
    private final EventsRepository eventsRepository;
    final BotConfig config;
    static final String HELP_MESSAGE = "Привет!\nС помощью этого бота можно" +
            " посмотреть список ближайших анонсированных мероприятий, узнать, где и когда они проходят," +
            " а также зарегистрироваться и пойти на интересующие лично тебя (если, конечно, есть свободные места!)" +
            "\n\nПросматривать афишу и регистрироваться можно на вкладке /events," +
            " командой /signed можно увидеть список мероприятий, на которые ты зарегистрирован(а)," +
            " если нужно отменить регистрацию, то напиши мне /cancel." +
            "\n\nЕсли мероприятие отменится - я обязательно тебе сообщу!\n" +
            "Также я предусмотрел возможность изменения регистрационных данных на вкладке /edit.";

    static final String REG_YES = "REG_YES_BUTTON";
    static final String REG_NO = "REG_NO_BUTTON";
    static final String CANCEL_REG_YES = "CANCEL_REG_YES";
    static final String CANCEL_REG_NO = "CANCEL_REG_NO";

    public TeleBotService (BotConfig config, PeopleRepository peopleRepository, EventsRepository eventsRepository) {
        this.config = config;
        this.peopleRepository = peopleRepository;
        this.eventsRepository = eventsRepository;
        List<BotCommand> commandsList = new ArrayList<>();
        commandsList.add(new BotCommand("/start", "Начать общение с ботом"));
        commandsList.add(new BotCommand("/events", "Список доступных мероприятий"));
        commandsList.add(new BotCommand("/signed", "Моя афиша"));
        commandsList.add(new BotCommand("/cancel", "Отменить регистрацию"));
        commandsList.add(new BotCommand("/help", "Помощь"));
        commandsList.add(new BotCommand("/edit", "Изменить данные"));
        try {
            this.execute(new SetMyCommands(commandsList, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error while initializing bot's commands list " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {

        if(update.hasMessage() && update.getMessage().hasText()) {
            String textMessage = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (textMessage.contains("/send") && config.getOwnerId() == chatId) {
                var textToSend = EmojiParser.parseToUnicode(textMessage.substring(textMessage.indexOf(" ")));
                var users = peopleRepository.findAll();
                for (Person person: users) {
                    prepareAndSendMessage(person.getChatId(), textToSend);
                }
            }

            else {
                switch (textMessage) {
                    case "/start":
                        startCommand(update.getMessage(), chatId, update.getMessage().getChat().getFirstName());
                        break;
                    case "Ближайшие мероприятия":
                    case "/events":
                        getAllEvents(chatId);
                        break;
                    case "/help":
                        sendMessage(chatId, HELP_MESSAGE);
                        break;
                    case "Мои мероприятия":
                    case "/signed":
                        getEventByPerson(chatId);
                        break;
                    case "Изменить данные":
                    case "/edit":
                    default:
                        if (peopleRepository.findByChatId(chatId).isEmpty()) {
                            prepareAndSendMessage(chatId, "Сначала нужно зарегистрироваться. Отправь команду /start");
                        } else {
                            prepareAndSendMessage(chatId, EmojiParser.parseToUnicode("Извини, этого я не понимаю " + ":pensive:" + "\nВоспользуйся меню!"));
                        }
                }
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackData.contains(REG_YES)) {
                registerForEvent(chatId, Long.parseLong(callbackData.replaceAll("\\D+", "")));
                String text = "Вы зарегистрировались. Мероприятие отображено в Вашем списке";
                executeEditMessageText(text, chatId, messageId);
//                executeMessage(new SendMessage(String.valueOf(chatId), text));
            } else if (callbackData.contains(REG_NO)) {
                String text = "Возврат в меню";
                executeEditMessageText(text, chatId, messageId);
            }
            else if (callbackData.contains(CANCEL_REG_YES)) {
                long eventId = eventsRepository.findById(Long.parseLong(callbackData.replaceAll("\\D+", ""))).get().getEventId();
                String text = "Регистрация на мероприятие отменена";
                executeEditMessageText(text, chatId, messageId);
                cancelRegistration(chatId, eventId);
            }
            else if (callbackData.contains(CANCEL_REG_NO)) {
                String text = "Возврат в меню";
                executeEditMessageText(text, chatId, messageId);
            }
            else if (eventsRepository.findById(Long.parseLong(callbackData)).isPresent()) {
                if (checkIfRegistered(chatId, Long.parseLong(callbackData))) {
                    Event eventToShow = eventsRepository.findById(Long.parseLong(callbackData)).orElse(null);
                    String text = "Мероприятие: " + eventToShow.getTitle() +
                            "\n" + eventToShow.getDescription() +
                            "\nКоличество мест: " + eventToShow.getSeats() +
                            "\nДата и время: " + eventToShow.getDate()
                            + "\nВы уже зарегистрированы на это мероприятие. Желаете отменить регистрацию?";

                    SendMessage message = new SendMessage();
                    message.setChatId(String.valueOf(chatId));
                    message.setText(text);
                    InlineKeyboardMarkup inlineMarkup = new InlineKeyboardMarkup();
                    List<List<InlineKeyboardButton>> inlineRows = new ArrayList<>();
                    List<InlineKeyboardButton> inlineRow = new ArrayList<>();
                    var yesButton = new InlineKeyboardButton();
                    yesButton.setText("Отменить регистрацию");
                    yesButton.setCallbackData(CANCEL_REG_YES + eventToShow.getEventId());

                    var noButton = new InlineKeyboardButton();
                    noButton.setText("Вернуться в меню");
                    noButton.setCallbackData(CANCEL_REG_NO + eventToShow.getEventId());

                    inlineRow.add(yesButton);
                    inlineRow.add(noButton);
                    inlineRows.add(inlineRow);
                    inlineMarkup.setKeyboard(inlineRows);
                    message.setReplyMarkup(inlineMarkup);

                    executeMessage(message);
                } else {
                    Event eventToShow = eventsRepository.findById(Long.parseLong(callbackData)).orElse(null);
//                    System.out.println(callbackData + " " + chatId);
                    String text = "Мероприятие: " + eventToShow.getTitle() +
                            "\n" + eventToShow.getDescription() +
                            "\nКоличество мест: " + eventToShow.getSeats() +
                            "\nДата и время: " + eventToShow.getDate()
                            + "\nЖелаете записаться?";

                    SendMessage message = new SendMessage();
                    message.setChatId(String.valueOf(chatId));
                    message.setText(text);
                    InlineKeyboardMarkup inlineMarkup = new InlineKeyboardMarkup();
                    List<List<InlineKeyboardButton>> inlineRows = new ArrayList<>();
                    List<InlineKeyboardButton> inlineRow = new ArrayList<>();
                    var yesButton = new InlineKeyboardButton();
                    yesButton.setText("Да");
                    yesButton.setCallbackData(REG_YES + eventToShow.getEventId());

                    var noButton = new InlineKeyboardButton();
                    noButton.setText("Нет");
                    noButton.setCallbackData(REG_NO + eventToShow.getEventId());

                    inlineRow.add(yesButton);
                    inlineRow.add(noButton);
                    inlineRows.add(inlineRow);
                    inlineMarkup.setKeyboard(inlineRows);
                    message.setReplyMarkup(inlineMarkup);

                    executeMessage(message);
                }
            }
        }
    }

    private void registerUser(Message message) {
        if (peopleRepository.findByChatId(message.getChatId()).isEmpty()) {
            var chatId = message.getChatId();
            var chat = message.getChat();

            Person person = new Person();
            person.setChatId(chatId);
            person.setFirstName(chat.getFirstName());
            person.setLastName(chat.getLastName());
            person.setUserName(chat.getUserName());
            person.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            peopleRepository.save(person);
            log.info("Saved user in DB: " + person);
        }
    }

    private void getAllEvents(long chatId) {
        List<Event> events = eventsRepository.findAll();
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Список предстоящих мероприятий ниже. Для просмотра информации нажмите на интересующее событие.");
        InlineKeyboardMarkup inlineMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> inlineRows = new ArrayList<>();

        for(Event event: events) {
            List<InlineKeyboardButton> inlineRow = new ArrayList<>();
            var eventButton = new InlineKeyboardButton();
            eventButton.setText(event.getTitle());
            eventButton.setCallbackData(String.valueOf(event.getEventId()));
            inlineRow.add(eventButton);
            inlineRows.add(inlineRow);
        }

        inlineMarkup.setKeyboard(inlineRows);
        message.setReplyMarkup(inlineMarkup);
        executeMessage(message);
    }

    public void getEventByPerson (long chatId) {
        List<Event> events = peopleRepository.findByChatId(chatId).get(0).getEvents();
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Список мероприятий, на которые вы зарегистрированы." +
                "\nДля отмены регистрации нажмите на мероприятие");
        InlineKeyboardMarkup inlineMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> inlineRows = new ArrayList<>();

        for(Event event: events) {
            List<InlineKeyboardButton> inlineRow = new ArrayList<>();
            var eventButton = new InlineKeyboardButton();
            eventButton.setText(event.getTitle());
            eventButton.setCallbackData(String.valueOf(event.getEventId()));
            inlineRow.add(eventButton);
            inlineRows.add(inlineRow);
        }

        inlineMarkup.setKeyboard(inlineRows);
        message.setReplyMarkup(inlineMarkup);
        executeMessage(message);
    }

    public void registerForEvent (long chatId, long eventId) {
        Event eventToRegister = eventsRepository.findById(eventId).orElse(null);
        Person personToRegister = peopleRepository.findByChatId(chatId).get(0);
        personToRegister.getEvents().add(eventToRegister);
        eventToRegister.setSeats(eventToRegister.getSeats() - 1);
        peopleRepository.save(personToRegister);
        eventsRepository.save(eventToRegister);
        log.info("Registered user with chatId " + chatId + " on event with id " + eventId);
    }

    public void cancelRegistration(long chatId, long eventId) {
        Event eventToCancelReg = eventsRepository.findById(eventId).orElse(null);
        Person personToCancelReg = peopleRepository.findByChatId(chatId).get(0);
        personToCancelReg.getEvents().remove(eventToCancelReg);
        peopleRepository.save(personToCancelReg);
        eventToCancelReg.getPeople().remove(personToCancelReg);
        eventToCancelReg.setSeats(eventToCancelReg.getSeats() + 1);
        eventsRepository.save(eventToCancelReg);
        log.info("Deleted registration for user with chatId " + chatId + " for event with id " + eventId);
    }

    private void startCommand(Message message, long chatId, String name) {
        if (!peopleRepository.findByChatId(chatId).isEmpty()) {
            String answer = EmojiParser.parseToUnicode("Ты уже зарегистрирован(а). :white_check_mark:\nСписок доступных команд можно посмотреть в меню," +
                            " или отправь /help, чтобы получить справку.");
            sendMessage(chatId, answer);
        }
        else {
            registerUser(message);
            String answer = EmojiParser.parseToUnicode("Привет, " + name + ", поздравляю с регистрацией! :blush:\nОткрой меню, чтобы увидеть список команд," +
                    " или отправь /help, чтобы получить справку.");
            sendMessage(chatId, answer);
            log.info("Replied on start to newly registered user " + name);
        }
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Ближайшие мероприятия");
        row.add("Мои мероприятия");
        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add("Помощь");
        row.add("Изменить данные");
        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add("Режим админа");
        keyboardRows.add(row);
        keyboardMarkup.setKeyboard(keyboardRows);
        keyboardMarkup.setResizeKeyboard(true);

        message.setReplyMarkup(keyboardMarkup);

        executeMessage(message);
    }

    private void executeEditMessageText (String text, long chatId, long messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId(Integer.parseInt(String.valueOf(messageId)));

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error while sending an answer with inline button: " + e.getMessage());
        }
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        }
        catch (TelegramApiException e) {
            log.error("Error while sending message: " + e.getMessage());
        }
    }

    private void prepareAndSendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }

    private boolean checkIfRegistered(long chatId, long eventId) {
        Person personToCheck = peopleRepository.findByChatId(chatId).get(0);
        List<Event> events = personToCheck.getEvents();
        for (Event event : events) {
            if (event.getEventId() == eventId)
                return true;
        }
        return false;
    }
}