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

import android.os.Handler;
import android.content.Intent;
import android.widget.Toast;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private TextView tvStats;
    // Добавьте эту переменную в объявлениях переменных класса
    private String exportContent = "";
    private Button btnSmoke, btnHistory;

    // Ключи для сохранения данных
    private static final String PREFS_NAME = "SmokingStats";
    private static final String KEY_DAILY_COUNT = "daily_count";
    private static final String KEY_DAILY_TIME = "daily_time";
    private static final String KEY_LAST_DATE = "last_date";
    private static final String KEY_ALL_SESSIONS = "all_sessions";
    private static final String KEY_PURPOSE_LIST = "purpose_list";
    private static final String KEY_ALL_TIME_STATS = "all_time_stats";

    // Новые переменные для статистики
    private int allTimeCigarettes = 0;
    private long allTimeSmokingTime = 0;

    private int dailyCigaretteCount = 0;
    private long dailySmokingTime = 0; // в минутах
    private List<String> smokingSessions = new ArrayList<>();
    private List<String> purposeList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Обработчик кнопки "Экспорт"
        Button btnExport = findViewById(R.id.btnExport);
        btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showExportDialog();
            }
        });

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
        builder.setMessage("Запустите таймер когда начнете курить");
        builder.setPositiveButton("Запустить таймер", (dialog, which) -> {
            startTimerDialog(purpose);
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    // Диалог работающего таймера
    private void startTimerDialog(String purpose) {
        // Создаем кастомный диалог с таймером
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Создаем layout для таймера
        View timerView = getLayoutInflater().inflate(R.layout.dialog_timer, null);

        TextView tvTimer = timerView.findViewById(R.id.tvTimer);
        Button btnStop = timerView.findViewById(R.id.btnStopTimer);

        // Таймер
        long[] startTime = {System.currentTimeMillis()};
        final int[] timerInterval = {0};

        // Обновляем таймер каждую секунду
        timerInterval[0] = timerInterval[0] + 1; // Чтобы избежать дублирования переменной
        final Handler handler = new Handler();
        final Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsedTime = System.currentTimeMillis() - startTime[0];
                long seconds = elapsedTime / 1000;
                long minutes = seconds / 60;
                seconds = seconds % 60;

                String timeText = String.format("%02d:%02d", minutes, seconds);
                tvTimer.setText(timeText);

                handler.postDelayed(this, 1000);
            }
        };

        // Запускаем таймер
        handler.postDelayed(timerRunnable, 0);

        builder.setView(timerView);
        builder.setTitle("Таймер работает...");
        builder.setCancelable(false);

        AlertDialog timerDialog = builder.create();

        // Обработчик кнопки остановки
        btnStop.setOnClickListener(v -> {
            handler.removeCallbacks(timerRunnable);
            long elapsedTime = System.currentTimeMillis() - startTime[0];
            int minutes = (int) (elapsedTime / 1000 / 60);
            if (minutes < 1) minutes = 1; // Минимум 1 минута

            timerDialog.dismiss();
            completeSmokingSession(minutes, purpose);
        });

        timerDialog.show();
    }

    // Завершение сессии курения
    private void completeSmokingSession(int minutes, String purpose) {
        dailyCigaretteCount++;
        dailySmokingTime += minutes;

        // Сохраняем детали сессии
        String timestamp = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date());
        String session = timestamp + " - " + minutes + " мин. (" + purpose + ")";
        smokingSessions.add(session);

        // Пересчитываем общую статистику
        recalculateAllTimeStats();

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
            new AlertDialog.Builder(this).setTitle("История").setMessage("Пока нет записей о курении").setPositiveButton("OK", null).show();
            return;
        }

        StringBuilder historyText = new StringBuilder();
        historyText.append("Всего сессий: ").append(smokingSessions.size()).append("\n\n");

        for (String session : smokingSessions) {
            historyText.append("• ").append(session).append("\n\n");
        }

        historyText.append("---\n").append("Всего сигарет: ").append(smokingSessions.size()).append("\n").append("Общее время: ").append(dailySmokingTime).append(" минут");

        new AlertDialog.Builder(this).setTitle("Вся история").setMessage(historyText.toString()).setPositiveButton("OK", null).show();
    }

    // Обновление отображения статистики
    private void updateStatsDisplay() {
        // Статистика за сегодня
        String todayStats = "Сегодня:\n" + "Сигарет: " + dailyCigaretteCount + "\n" + "Время: " + dailySmokingTime + "м\n\n";

        // Статистика за неделю
        WeekStats weekStats = getWeekStats();
        String weekStatsText = "За неделю:\n" + "Сигарет: " + weekStats.count + "\n" + "Время: " + weekStats.time + "м\n\n";

        // Статистика за месяц
        MonthStats monthStats = getMonthStats();
        String monthStatsText = "За месяц:\n" + "Сигарет: " + monthStats.count + "\n" + "Время: " + monthStats.time + "м\n\n";

        // Вся статистика
        String allTimeStats = "Всего:\n" + "Сигарет: " + allTimeCigarettes + "\n" + "Время: " + allTimeSmokingTime + "м";

        String stats = todayStats + weekStatsText + monthStatsText + allTimeStats;
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

        // Восстанавливаем общую статистику из истории
        recalculateAllTimeStats();
    }

    // Пересчет общей статистики из истории
    private void recalculateAllTimeStats() {
        allTimeCigarettes = smokingSessions.size();
        allTimeSmokingTime = 0;

        for (String session : smokingSessions) {
            try {
                String timePart = session.split(" - ")[1];
                String timeStr = timePart.split(" ")[0];
                allTimeSmokingTime += Integer.parseInt(timeStr);
            } catch (Exception e) {
                System.out.println("Ошибка пересчета сессии: " + session);
            }
        }
    }

    // Классы для хранения статистики
    private static class WeekStats {
        int count;
        long time;

        WeekStats(int count, long time) {
            this.count = count;
            this.time = time;
        }
    }

    private static class MonthStats {
        int count;
        long time;

        MonthStats(int count, long time) {
            this.count = count;
            this.time = time;
        }
    }

    // Статистика за неделю
    private WeekStats getWeekStats() {
        int weekCount = 0;
        long weekTime = 0;

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        Date weekAgo = calendar.getTime();

        for (String session : smokingSessions) {
            try {
                // Извлекаем дату и время из строки сессии (формат: "01.01.2024 12:30 - 5 мин. (Причина)")
                String dateTimeStr = session.split(" - ")[0]; // "01.01.2024 12:30"
                Date sessionDate = sdf.parse(dateTimeStr);

                if (sessionDate != null && sessionDate.after(weekAgo)) {
                    weekCount++;
                    // Извлекаем время из сессии (формат: "5 мин.")
                    String timePart = session.split(" - ")[1]; // "5 мин. (Причина)"
                    String timeStr = timePart.split(" ")[0]; // "5"
                    weekTime += Integer.parseInt(timeStr);
                }
            } catch (Exception e) {
                // Если ошибка парсинга, пропускаем эту сессию
                System.out.println("Ошибка парсинга сессии: " + session);
            }
        }

        return new WeekStats(weekCount, weekTime);
    }

    // Статистика за месяц
    private MonthStats getMonthStats() {
        int monthCount = 0;
        long monthTime = 0;

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        Date monthAgo = calendar.getTime();

        for (String session : smokingSessions) {
            try {
                String dateTimeStr = session.split(" - ")[0];
                Date sessionDate = sdf.parse(dateTimeStr);

                if (sessionDate != null && sessionDate.after(monthAgo)) {
                    monthCount++;
                    String timePart = session.split(" - ")[1];
                    String timeStr = timePart.split(" ")[0];
                    monthTime += Integer.parseInt(timeStr);
                }
            } catch (Exception e) {
                System.out.println("Ошибка парсинга сессии: " + session);
            }
        }

        return new MonthStats(monthCount, monthTime);
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
        editor.putStringSet(KEY_PURPOSE_LIST, new HashSet<>(purposeList));

        editor.apply();
    }

    // Сохранение списка причин
    private void savePurposeList() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(KEY_PURPOSE_LIST, new HashSet<>(purposeList));
        editor.apply();
    }

    // Диалог экспорта данных
    private void showExportDialog() {
        String[] exportOptions = {"Текстовый файл", "Поделиться статистикой"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Экспорт данных");
        builder.setItems(exportOptions, (dialog, which) -> {
            if (which == 0) {
                exportToFile();
            } else {
                shareStatistics();
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    // Экспорт в файл
    private void exportToFile() {
        try {
            String fileName = "smoking_stats_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".txt";

            String content = generateExportContent();

            // Создаем intent для сохранения файла
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TITLE, fileName);

            startActivityForResult(intent, 1);

            // Сохраняем содержимое во временную переменную для использования в onActivityResult
            exportContent = content;

        } catch (Exception e) {
            Toast.makeText(this, "Ошибка экспорта: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Генерация содержимого для экспорта
    private String generateExportContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== СТАТИСТИКА КУРЕНИЯ ===\n\n");

        sb.append("ОБЩАЯ СТАТИСТИКА:\n");
        sb.append("Всего сигарет: ").append(allTimeCigarettes).append("\n");
        sb.append("Общее время: ").append(allTimeSmokingTime).append(" минут\n");
        sb.append("Среднее в день: ").append(allTimeCigarettes > 0 ? String.format("%.1f", (float) allTimeCigarettes / smokingSessions.size()) : 0).append(" сигарет\n\n");

        WeekStats weekStats = getWeekStats();
        sb.append("ЗА НЕДЕЛЮ:\n");
        sb.append("Сигарет: ").append(weekStats.count).append("\n");
        sb.append("Время: ").append(weekStats.time).append(" минут\n\n");

        MonthStats monthStats = getMonthStats();
        sb.append("ЗА МЕСЯЦ:\n");
        sb.append("Сигарет: ").append(monthStats.count).append("\n");
        sb.append("Время: ").append(monthStats.time).append(" минут\n\n");

        sb.append("ПОСЛЕДНИЕ СЕССИИ:\n");
        int count = 0;
        for (int i = smokingSessions.size() - 1; i >= 0 && count < 50; i--, count++) {
            sb.append(smokingSessions.get(i)).append("\n");
        }

        sb.append("\nЭкспортировано: ").append(new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date()));

        return sb.toString();
    }

    // Поделиться статистикой
    private void shareStatistics() {
        String shareText = generateExportContent();

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Моя статистика курения");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        startActivity(Intent.createChooser(shareIntent, "Поделиться статистикой"));
    }
}