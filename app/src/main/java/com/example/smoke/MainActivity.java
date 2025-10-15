package com.example.smoke;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private TextView tvStats;
    private Button btnSmoke, btnHistory;

    // Ключи для сохранения данных
    private static final String PREFS_NAME = "SmokingStats";
    private static final String KEY_DAILY_COUNT = "daily_count";
    private static final String KEY_DAILY_TIME = "daily_time";
    private static final String KEY_LAST_DATE = "last_date";
    private static final String KEY_ALL_SESSIONS = "all_sessions";
    private static final String KEY_PURPOSE_LIST = "purpose_list";

    private int dailyCigaretteCount = 0;
    private long dailySmokingTime = 0; // в минутах
    private List<String> smokingSessions = new ArrayList<>();
    private List<String> purposeList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Находим все элементы на экране
        tvStats = findViewById(R.id.tvStats);
        btnSmoke = findViewById(R.id.btnSmoke);
        btnHistory = findViewById(R.id.btnHistory);

        // Загружаем сохраненные данные
        loadAllData();

        // Обработчик кнопки "Курить"
        btnSmoke.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPurposeDialog();
            }
        });

        // Обработчик кнопки "История"
        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHistory();
            }
        });

        updateStatsDisplay();
    }

    // Диалог выбора причины курения
    private void showPurposeDialog() {
        // Создаем список причин, если он пустой
        if (purposeList.isEmpty()) {
            purposeList.add("Снятие стресса");
            purposeList.add("Перекур");
            purposeList.add("От скуки");
            purposeList.add("С компанией");
            purposeList.add("По привычке");
        }

        // Создаем диалог с выбором причины
        String[] purposes = purposeList.toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Зачем ты куришь?");
        builder.setItems(purposes, (dialog, which) -> {
            String selectedPurpose = purposeList.get(which);
            showMethodDialog(selectedPurpose);
        });

        // Кнопка для добавления новой причины
        builder.setNeutralButton("Добавить свою причину", (dialog, which) -> {
            showAddPurposeDialog();
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    // Диалог добавления новой причины
    private void showAddPurposeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Добавить причину");

        // Поле для ввода текста
        final View customLayout = getLayoutInflater().inflate(R.layout.dialog_input, null);
        builder.setView(customLayout);

        builder.setPositiveButton("Добавить", (dialog, which) -> {
            TextView input = customLayout.findViewById(R.id.etInput);
            String newPurpose = input.getText().toString().trim();
            if (!newPurpose.isEmpty() && !purposeList.contains(newPurpose)) {
                purposeList.add(newPurpose);
                savePurposeList();
                showPurposeDialog(); // Показываем обновленный список
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    // Диалог выбора способа записи
    private void showMethodDialog(String purpose) {
        String[] methods = {"1 сигарета (4 минуты)", "Запустить таймер"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Способ записи");
        builder.setItems(methods, (dialog, which) -> {
            if (which == 0) {
                // 1 сигарета - сразу добавляем 4 минуты
                completeSmokingSession(4, purpose);
            } else {
                // Таймер
                showTimerDialog(purpose);
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    // Диалог таймера
    private void showTimerDialog(String purpose) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Таймер курения");
        builder.setMessage("Запустите таймер когда начнете курить, и остановите когда закончите.");
        builder.setPositiveButton("Запустить", (dialog, which) -> {
            // Здесь должен быть настоящий таймер, но для простоты используем фиксированное время
            showTimerRunningDialog(purpose);
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    // Диалог работающего таймера
    private void showTimerRunningDialog(String purpose) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Таймер работает...");
        builder.setMessage("Курите... Нажмите 'Завершить' когда закончите.");
        builder.setPositiveButton("Завершить", (dialog, which) -> {
            // Спросим сколько времени заняло
            showTimeInputDialog(purpose);
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    // Диалог ввода времени
    private void showTimeInputDialog(String purpose) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Сколько минут вы курили?");

        final View customLayout = getLayoutInflater().inflate(R.layout.dialog_input, null);
        TextView input = customLayout.findViewById(R.id.etInput);
        input.setHint("Введите минуты");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        builder.setView(customLayout);

        builder.setPositiveButton("Готово", (dialog, which) -> {
            try {
                int minutes = Integer.parseInt(input.getText().toString());
                completeSmokingSession(minutes, purpose);
            } catch (NumberFormatException e) {
                completeSmokingSession(5, purpose); // По умолчанию 5 минут
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    // Завершение сессии курения
    private void completeSmokingSession(int minutes, String purpose) {
        dailyCigaretteCount++;
        dailySmokingTime += minutes;

        // Сохраняем детали сессии
        String timestamp = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date());
        String session = timestamp + " - " + minutes + " мин. (" + purpose + ")";
        smokingSessions.add(session);

        saveAllData();
        updateStatsDisplay();

        // Показываем подтверждение
        String message = "Добавлено: " + minutes + " минут\nПричина: " + purpose;
        new AlertDialog.Builder(this)
                .setTitle("Записано!")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    // Показ истории
    private void showHistory() {
        if (smokingSessions.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("История")
                    .setMessage("Пока нет записей о курении")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        StringBuilder historyText = new StringBuilder();
        historyText.append("Всего сессий: ").append(smokingSessions.size()).append("\n\n");

        for (String session : smokingSessions) {
            historyText.append("• ").append(session).append("\n\n");
        }

        historyText.append("---\n")
                .append("Всего сигарет: ").append(smokingSessions.size()).append("\n")
                .append("Общее время: ").append(dailySmokingTime).append(" минут");

        new AlertDialog.Builder(this)
                .setTitle("Вся история")
                .setMessage(historyText.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    // Обновление отображения статистики
    private void updateStatsDisplay() {
        String stats = "Статистика за сегодня:\n" +
                "Сигарет: " + dailyCigaretteCount + "\n" +
                "Время: " + dailySmokingTime + "м";
        tvStats.setText(stats);
    }

    // Загрузка всех данных
    private void loadAllData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastDate = prefs.getString(KEY_LAST_DATE, "");

        // Сбрасываем дневную статистику если дата изменилась
        if (!lastDate.equals(today)) {
            dailyCigaretteCount = 0;
            dailySmokingTime = 0;
        } else {
            dailyCigaretteCount = prefs.getInt(KEY_DAILY_COUNT, 0);
            dailySmokingTime = prefs.getLong(KEY_DAILY_TIME, 0);
        }

        // Загружаем историю сессий
        Set<String> sessionsSet = prefs.getStringSet(KEY_ALL_SESSIONS, new HashSet<>());
        smokingSessions = new ArrayList<>(sessionsSet);

        // Загружаем список причин
        Set<String> purposesSet = prefs.getStringSet(KEY_PURPOSE_LIST, new HashSet<>());
        purposeList = new ArrayList<>(purposesSet);
    }

    // Сохранение всех данных
    private void saveAllData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        editor.putInt(KEY_DAILY_COUNT, dailyCigaretteCount);
        editor.putLong(KEY_DAILY_TIME, dailySmokingTime);
        editor.putString(KEY_LAST_DATE, today);
        editor.putStringSet(KEY_ALL_SESSIONS, new HashSet<>(smokingSessions));

        editor.apply();
    }

    // Сохранение списка причин
    private void savePurposeList() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(KEY_PURPOSE_LIST, new HashSet<>(purposeList));
        editor.apply();
    }
}