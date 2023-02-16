package ru.ratnikov.ShowBillBot.service;

import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.ratnikov.ShowBillBot.Util.BotUtil;
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
public class ShowbillBot extends TelegramLongPollingBot {

    private final PeopleRepository peopleRepository;
    private final EventsRepository eventsRepository;
    private final String botUsername;
    private final String botToken;
    private final String botOwner;

    public ShowbillBot(
            @Value("${bot.name}") String botUsername,
            @Value("${bot.token}") String botToken,
            @Value("${bot.name}")String botOwner,
            PeopleRepository peopleRepository,
            EventsRepository eventsRepository) {

        this.botUsername = botUsername;
        this.botToken = botToken;
        this.peopleRepository = peopleRepository;
        this.eventsRepository = eventsRepository;
        this.botOwner = botOwner;

        initializeMenuList();
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    private void initializeMenuList() {
        List<BotCommand> commandsList = BotUtil.getBotCommandsList();
        try {
            this.execute(new SetMyCommands(commandsList, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error while initializing bot's commands list " + e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {

        if(update.hasMessage() && update.getMessage().hasText()) {
            String incomingMessageText = update.getMessage().getText();
            long incomingChatId = update.getMessage().getChatId();

            if (checkIfUserIsRegistered(incomingChatId)) {
                if (incomingMessageText.contains("/send") && Long.parseLong(botOwner) == incomingChatId) {
                    sendMessageToAll(incomingMessageText);
                } else {
                    handleRegisteredUser (incomingChatId, incomingMessageText, update);
                }
            }
            else {
                handleUnregisteredUser (update.getMessage());
            }
        } else if (update.hasCallbackQuery()) {
            processCallbackQuery(update);
        }
    }

    private void processCallbackQuery(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        long messageId = update.getCallbackQuery().getMessage().getMessageId();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        if (callbackData.contains(BotUtil.B_REG_YES)) {
            long eventId = Long.parseLong(callbackData.replaceAll("\\D+", ""));
            registerForEvent(chatId, eventId, messageId);
            executeMessage(BotUtil.createMessageTemplate(chatId, BotUtil.EVENT_REGISTRATION_SUCCESS));
        } else if (callbackData.contains(BotUtil.B_REG_NO)) {
            long eventId = Long.parseLong(callbackData.replaceAll("\\D+", ""));
            executeEditMessageText(getEventInfoNoReg(eventId), chatId, messageId);
            executeMessage(BotUtil.createMessageTemplate(chatId, BotUtil.BACK_TO_MENU));
        }
        else if (callbackData.contains(BotUtil.B_CANCEL_REG_YES)) {
            long eventId = eventsRepository.findById(Long.parseLong(callbackData.replaceAll("\\D+", ""))).get().getEventId();
            cancelRegistration(chatId, eventId);
            executeEditMessageText(BotUtil.REGISTRATION_CANCELLED, chatId, messageId);
        }
        else if (callbackData.contains(BotUtil.B_CANCEL_REG_NO)) {
            executeEditMessageText(BotUtil.BACK_TO_MENU, chatId, messageId);
        }
        else if (eventsRepository.findById(Long.parseLong(callbackData)).isPresent()) {
            long eventId = Long.parseLong(callbackData);
            if (checkIfRegisteredForEvent(chatId, Long.parseLong(callbackData))) {
                processRegisteredForEventPerson(chatId, eventId);
            } else {
                processUnregisteredForEventPerson(chatId, eventId);
            }
        }
    }

    private void processUnregisteredForEventPerson(long chatId, long eventId) {
        Event eventToShow = eventsRepository.findById(eventId).orElse(null);

        if (eventToShow != null) {
            String text = getEventInfoNoReg(eventId);
            SendMessage message = BotUtil.createMessageTemplate(chatId, text);
            InlineKeyboardMarkup inlineMarkup = createTwoInlinesForMessage(
                    "Да", BotUtil.B_REG_YES + eventToShow.getEventId(),
                    "Нет", BotUtil.B_REG_NO + eventToShow.getEventId());
            message.setReplyMarkup(inlineMarkup);

            executeMessage(message);
        }
        else {
            log.error("Tried to show event with id " + eventId + " which doesn't exists in db");
        }
    }

    private void processRegisteredForEventPerson(long chatId, long eventId) {
        Event eventToShow = eventsRepository.findById(eventId).orElse(null);

        if (eventToShow != null) {
            String text = getEventInfoWithReg(eventId);
            SendMessage message = BotUtil.createMessageTemplate(chatId, text);

            InlineKeyboardMarkup inlineMarkup = createTwoInlinesForMessage(
                    "Отменить регистрацию", BotUtil.B_CANCEL_REG_YES + eventToShow.getEventId(),
                    "Вернуться в меню", BotUtil.B_CANCEL_REG_NO + eventToShow.getEventId());
            message.setReplyMarkup(inlineMarkup);

            executeMessage(message);
        }
        else {
            log.error("Tried to show event with id " + eventId + " which doesn't exists in db");
        }
    }

    private void handleRegisteredUser(long incomingChatId, String incomingMessageText, Update update) {
        switch (incomingMessageText) {
            case BotUtil.START_COMM:
            case BotUtil.START_COMM_TEXT_1:
            case BotUtil.START_COMM_TEXT_2:
                onStartCommand(update.getMessage());
                break;
            case BotUtil.EVENTS_COMM:
            case BotUtil.EVENTS_COMM_TEXT:
                getAllEvents(incomingChatId);
                break;
            case BotUtil.HELP_COMM:
            case BotUtil.HELP_COMM_TEXT:
                sendMessageGetMenu(incomingChatId, BotUtil.HELP_MESSAGE);
                break;
            case BotUtil.SIGNED_COMM:
            case BotUtil.SIGNED_COMM_TEXT:
                getEventByPerson(incomingChatId);
                break;
            case BotUtil.EDIT_COMM:
            case BotUtil.EDIT_COMM_TEXT:
            case BotUtil.ADMIN_MODE_COMM:
            case BotUtil.ADMIN_MODE_COMM_TEXT:
                prepareAndSendMessage(incomingChatId, BotUtil.NOT_READY);
                break;
            default:
                prepareAndSendMessage(incomingChatId, BotUtil.UNKNOWN_COMMAND);
        }
    }

    private void handleUnregisteredUser(Message message) {
        switch (message.getText()) {
            case BotUtil.START_COMM:
            case BotUtil.START_COMM_TEXT_1:
            case BotUtil.START_COMM_TEXT_2:
                onStartCommand(message);
                break;
            case BotUtil.HELP_COMM:
            case BotUtil.HELP_COMM_TEXT:
                sendMessageGetMenu(message.getChatId(), BotUtil.HELP_MESSAGE);
                break;
            default:
                prepareAndSendMessage(message.getChatId(), BotUtil.NEED_TO_REGISTER);
        }
    }

    private void onStartCommand(Message message) {
        long chatId = message.getChatId();
        if (!peopleRepository.findByChatId(chatId).isEmpty()) {
            sendMessageGetMenu(chatId, BotUtil.ALREADY_REGISTERED);
        }
        else {
            registerUser(message);
            sendMessageGetMenu(chatId, "Привет, " + message.getChat().getFirstName() + BotUtil.BOT_REGISTRATION_SUCCESS);
            log.info("Replied on start to newly registered user " + message.getChat().getUserName());
        }
    }

    private void sendMessageToAll(String message) {
        var textToSend = EmojiParser.parseToUnicode(message.substring(message.indexOf(" ")));
        var users = peopleRepository.findAll();
        for (Person person: users) {
            prepareAndSendMessage(person.getChatId(), textToSend);
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
        SendMessage message = BotUtil.createMessageTemplate(chatId);
        message.setText(BotUtil.FOR_ALL_EVENT_LIST);
        BotUtil.prepareMessageWithEventsList(events, message);
        executeMessage(message);
    }

    public void getEventByPerson (long chatId) {
        List<Event> events = peopleRepository.findByChatId(chatId).get(0).getEvents();
        SendMessage message = BotUtil.createMessageTemplate(chatId);
        if (events.isEmpty()) {
            message.setText(BotUtil.FOR_NO_REG_FOR_EVENTS);
            executeMessage(message);
            getAllEvents(chatId);
        }
        else {
            message.setText(BotUtil.FOR_PERSON_EVENT_LIST);
            BotUtil.prepareMessageWithEventsList(events, message);
            executeMessage(message);
        }
    }

    public void registerForEvent (long chatId, long eventId, long messageId) {
        Event eventToRegister = eventsRepository.findById(eventId).orElse(null);
        if (eventToRegister != null) {
            Person personToRegister = peopleRepository.findByChatId(chatId).get(0);
            personToRegister.getEvents().add(eventToRegister);
            eventToRegister.setSeats(eventToRegister.getSeats() - 1);
            peopleRepository.save(personToRegister);
            eventsRepository.save(eventToRegister);
            executeEditMessageText(getEventInfoNoReg(eventId), chatId, messageId);
            log.info("Registered user with chatId " + chatId + " on event with id " + eventId);
        }
        else
            log.error("Tried to get event with id " + eventId + " which doesn't exists in db");
    }

    public void cancelRegistration(long chatId, long eventId) {
        Event eventToCancelReg = eventsRepository.findById(eventId).orElse(null);
        Person personToCancelReg = peopleRepository.findByChatId(chatId).get(0);
        if (personToCancelReg != null && eventToCancelReg != null) {
            personToCancelReg.getEvents().remove(eventToCancelReg);
            peopleRepository.save(personToCancelReg);
            eventToCancelReg.getPeople().remove(personToCancelReg);
            eventToCancelReg.setSeats(eventToCancelReg.getSeats() + 1);
            eventsRepository.save(eventToCancelReg);
            getAllEvents(chatId);
            log.info("Deleted registration for user with chatId " + chatId + " for event with id " + eventId);
        }
        else
            log.error("Tried to get event with id " + eventId + " and person with id " + chatId + "  which don't exists in db");
    }

    private void sendMessageGetMenu(long chatId, String textToSend) {
        SendMessage message = BotUtil.createMessageTemplateWithMenu(chatId, textToSend);
        executeMessage(message);
    }

    private void executeEditMessageText (String text, long chatId, long messageId) {
        EditMessageText message = BotUtil.createEditMessageTextTemplate(text, chatId, messageId);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error while editing a chat message: " + e.getMessage());
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

    private InlineKeyboardMarkup createTwoInlinesForMessage (String buttonOneText, String buttonOneCommand,
                                                             String buttonTwoText, String buttonTwoCommand) {
        InlineKeyboardMarkup inlineMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> inlineRows = new ArrayList<>();
        List<InlineKeyboardButton> inlineRow = new ArrayList<>();
        var yesButton = BotUtil.createInlineKeyboardButton(buttonOneText, buttonOneCommand);
        var noButton = BotUtil.createInlineKeyboardButton(buttonTwoText, buttonTwoCommand);

        inlineRow.add(yesButton);
        inlineRow.add(noButton);
        inlineRows.add(inlineRow);
        inlineMarkup.setKeyboard(inlineRows);

        return inlineMarkup;
    }

    private boolean checkIfUserIsRegistered(long chatId) {
        return !peopleRepository.findByChatId(chatId).isEmpty();
    }

    private boolean checkIfRegisteredForEvent(long chatId, long eventId) {
        Person personToCheck = peopleRepository.findByChatId(chatId).get(0);
        List<Event> events = personToCheck.getEvents();
        for (Event event : events) {
            if (event.getEventId() == eventId)
                return true;
        }
        return false;
    }

    private String getEventInfo (long eventId) {
        Event event = eventsRepository.findById(eventId).orElse(null);
        if (event != null) {
            return "Мероприятие: " + event.getTitle() +
                    "\n" + event.getDescription() +
                    "\nКоличество мест: " + event.getSeats() +
                    "\nДата и время: " + event.getDate();
        }
        else {
            log.error("Tried to get info for event with id " + eventId + " which doesn't exist ind db");
            return null;
        }
    }

    private String getEventInfoNoReg (long eventId) {
        return getEventInfo(eventId) + "\nЖелаете записаться?";
    }

    private String getEventInfoWithReg (long eventId) {
        return getEventInfo(eventId) + "\nВы уже зарегистрированы на это мероприятие. Желаете отменить регистрацию?";
    }
}