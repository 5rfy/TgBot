package com.example.tgbot.Service;

import com.example.tgbot.Config.BotConfig;
import com.example.tgbot.Model.Entity.User;
import com.example.tgbot.Model.Repository.UserRepository;
import org.springframework.stereotype.Component;
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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig botConfig;
    private final UserRepository userRepository;
    private static final String HELP_TEXT = """
            This bot is created to demonstrate Spring capabilities.

            You can execute commands rom the main menu on the left or by tapping command:

            Type /start to see a welcome message,

            Type /mydate to see data stored about yourself,

            Type /help to see this message again.

            """;
    private static final String YES_BUTTON = "YES_BUTTON";
    private static final String NO_BUTTON = "NO_BUTTON";

    public TelegramBot(BotConfig botConfig, UserRepository userRepository) {
        this.botConfig = botConfig;
        this.userRepository = userRepository;
        List<BotCommand> listOfCommands = new ArrayList<>(){{;
            add(new BotCommand("/start", "get a welcome message"));
            add(new BotCommand("/mydata", "get your data stored"));
            add(new BotCommand("/deletedata", "delete my data"));
            add(new BotCommand("/help", "info how to use this bot"));
            add(new BotCommand("/settings", "set your preferences"));
        }};
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
//            log.error("Error settings bots command list: " + e.getMessage());//TODO settings log
        }

    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }
    @Override
    public String getBotUsername() {
        return botConfig.getName();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start" -> {
                    registerUser(update.getMessage());
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                }
                case "/help" -> sendMessage(chatId, HELP_TEXT);
                case "/register" -> register(chatId);
                default -> sendMessage(chatId, "Sorry, command was not recognized");
            }
        } else if (update.hasCallbackQuery()) {

            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();


            if(callbackData.equals(YES_BUTTON)) {
                String text = "You pressed YES button";

                executeEditMessageText(text,chatId,messageId);
            } else if (callbackData.equals(NO_BUTTON)) {
                String text = "You pressed NO button";

                executeEditMessageText(text,chatId,messageId);
            }
        }
    }
    private void executeEditMessageText(String text, long chatId, long messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setText(text);
        message.setMessageId((int) messageId);

        try {
            execute(message);
        } catch (TelegramApiException e) {
//            log.error("Error occurred: " + e.getMessage());//TODO settings log
        }
    }
    private void register(long chatId) {

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Do you really want to register?");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        var yesButton = new InlineKeyboardButton();
        yesButton.setText("Yes");
        yesButton.setCallbackData(YES_BUTTON);

        var noButton = new InlineKeyboardButton();
        noButton.setText("No");
        noButton.setCallbackData(NO_BUTTON);

        rowInLine.add(yesButton);
        rowInLine.add(noButton);

        rowsInLine.add(rowInLine);

        markup.setKeyboard(rowsInLine);

        message.setReplyMarkup(markup);

        executeMessage(message);
    }

    private void startCommandReceived(long id, String name){
        String response = "Hi " + name + ", nice to meet you!";
//        log.info("Replied to user " + name);//TODO settings log
        sendMessage(id, response);

    }
    private void sendMessage(long chatId, String textToSend){
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);

        message.setReplyMarkup(keyboardMarkup());

        executeMessage(message);
    }
    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
//            log.error("Error occurred: " + e.getMessage());//TODO settings log
        }
    }

    private void registerUser(Message msg) {
        if (userRepository.findById(msg.getChatId()).isEmpty()) {

            var chatId = msg.getChatId();
            var chat = msg.getChat();

            User user = new User();

            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUsername(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
//            log.info("user saved: " + user);//TODO settings log
        }
    }

    private ReplyKeyboardMarkup keyboardMarkup() {

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> rowList = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
//      ________first row___________
        row.add("weather");
        row.add("get random joke");

        rowList.add(row);
//      ____________________________

        row = new KeyboardRow();
//      ________second row__________
        row.add("register");
        row.add("check_my_data");
        row.add("delete_my_data");

        rowList.add(row);
//      ____________________________

        keyboardMarkup.setKeyboard(rowList);
        return keyboardMarkup;
    }
}
