package com.example.smoke;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.widget.ArrayAdapter;

public class MainActivity extends AppCompatActivity {

    private TextView tvStats;
    // Добавьте эту переменную в объявлениях переменных класса
    private String exportContent = "";
    private Button btnSmoke, btnHistory, btnExport, btnImport;

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
    // Добавляем в переменные класса
    private float cigarettePackPrice = 0;
    private float cigarettePrice = 0;
    private static final String KEY_PACK_PRICE = "pack_price";
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
        btnExport = findViewById(R.id.btnExport);
        btnImport = findViewById(R.id.btnImport); // ← ЭТА СТРОКА ДОЛЖНА БЫТЬ!

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

        // Обработчик кнопки "Экспорт"
        btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showExportDialog();
            }
        });

        // Обработчик кнопки "Импорт" - ДОБАВЬ ЭТОТ КОД!
        btnImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImportDialog();
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
            savePurposeList();
        }

        // Создаем диалог с выбором причины
        String[] purposes = purposeList.toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Зачем ты куришь?");

        // Простой список без кастомного адаптера
        builder.setItems(purposes, (dialog, which) -> {
            String selectedPurpose = purposeList.get(which);
            showMethodDialog(selectedPurpose);
        });

        // Кнопка для добавления новой причины
        builder.setNeutralButton("Добавить свою причину", (dialog, which) -> {
            showAddPurposeDialog();
        });

        // Кнопка для управления причинами (удаление/редактирование)
        builder.setPositiveButton("Управление причинами", (dialog, which) -> {
            showManagePurposesDialog();
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    // Диалог управления причинами
    private void showManagePurposesDialog() {
        if (purposeList.isEmpty()) {
            Toast.makeText(this, "Список причин пуст", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] purposes = purposeList.toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Управление причинами");
        builder.setItems(purposes, (dialog, which) -> {
            // Показываем опции для выбранной причины
            showPurposeOptionsDialog(which);
        });
        builder.setPositiveButton("Добавить новую", (dialog, which) -> {
            showAddPurposeDialog();
        });
        builder.setNegativeButton("Назад", (dialog, which) -> {
            showPurposeDialog();
        });
        builder.show();
    }

    // Диалог опций для конкретной причины
    private void showPurposeOptionsDialog(int position) {
        String purpose = purposeList.get(position);

        String[] options = {"Удалить", "Переименовать", "Использовать"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(purpose);
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Удалить
                    showDeletePurposeDialog(position);
                    break;
                case 1: // Переименовать
                    showRenamePurposeDialog(position);
                    break;
                case 2: // Использовать
                    showMethodDialog(purpose);
                    break;
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    // Диалог переименования причины
    private void showRenamePurposeDialog(int position) {
        String oldPurpose = purposeList.get(position);

        View renameView = getLayoutInflater().inflate(R.layout.dialog_input, null);
        EditText etInput = renameView.findViewById(R.id.etInput);
        etInput.setText(oldPurpose);
        etInput.setSelection(oldPurpose.length());

        new AlertDialog.Builder(this)
                .setTitle("Переименовать причину")
                .setView(renameView)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String newPurpose = etInput.getText().toString().trim();
                    if (!newPurpose.isEmpty() && !purposeList.contains(newPurpose)) {
                        purposeList.set(position, newPurpose);
                        savePurposeList();
                        Toast.makeText(this, "Причина переименована", Toast.LENGTH_SHORT).show();
                        showManagePurposesDialog();
                    } else {
                        Toast.makeText(this, "Некорректное название", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    // Диалог удаления причины
    private void showDeletePurposeDialog(int position) {
        String purposeToDelete = purposeList.get(position);

        new AlertDialog.Builder(this)
                .setTitle("Удалить причину?")
                .setMessage("Удалить \"" + purposeToDelete + "\"?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    purposeList.remove(position);
                    savePurposeList();
                    Toast.makeText(this, "Причина удалена", Toast.LENGTH_SHORT).show();
                    showManagePurposesDialog();
                })
                .setNegativeButton("Отмена", null)
                .show();
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
        View timerView = getLayoutInflater().inflate(R.layout.dialog_timer, null);
        TextView tvTimer = timerView.findViewById(R.id.tvTimer);
        Button btnStop = timerView.findViewById(R.id.btnStopTimer);

        long[] startTime = {System.currentTimeMillis()};
        final Handler handler = new Handler();
        final boolean[] notificationShown = {false};

        final Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsedTime = System.currentTimeMillis() - startTime[0];
                long seconds = elapsedTime / 1000;
                long minutes = seconds / 60;
                seconds = seconds % 60;

                String timeText = String.format("%02d:%02d", minutes, seconds);
                tvTimer.setText(timeText);

                // Показываем уведомление после 30 минут
                if (minutes >= 30 && !notificationShown[0]) {
                    notificationShown[0] = true;
                    showLongSessionNotification();
                }

                handler.postDelayed(this, 1000);
            }
        };

        // Запускаем таймер
        handler.postDelayed(timerRunnable, 0);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(timerView);
        builder.setTitle("Таймер работает...");
        builder.setCancelable(false);

        AlertDialog timerDialog = builder.create();

        btnStop.setOnClickListener(v -> {
            handler.removeCallbacks(timerRunnable);
            long elapsedTime = System.currentTimeMillis() - startTime[0];
            int minutes = (int) (elapsedTime / 1000 / 60);
            if (minutes < 1) minutes = 1;

            timerDialog.dismiss();
            showCigaretteCountDialog(minutes, purpose);
        });

        timerDialog.show();
    }

    // Уведомление о долгой сессии
    private void showLongSessionNotification() {
        new AlertDialog.Builder(this)
                .setTitle("Всё ещё курите?")
                .setMessage("Сессия длится более 30 минут. Не забудьте остановить таймер!")
                .setPositiveButton("OK", null)
                .show();
    }

    // Диалог выбора количества сигарет
    private void showCigaretteCountDialog(int minutes, String purpose) {
        String[] options = {"Только одну", "Несколько сигарет"};

        new AlertDialog.Builder(this)
                .setTitle("Сколько сигарет выкурили?")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        completeSmokingSession(minutes, purpose, 1);
                    } else {
                        showMultipleCigarettesDialog(minutes, purpose);
                    }
                })
                .show();
    }

    // Диалог ввода количества сигарет
    private void showMultipleCigarettesDialog(int minutes, String purpose) {
        View countView = getLayoutInflater().inflate(R.layout.dialog_input, null);
        EditText etInput = countView.findViewById(R.id.etInput);
        etInput.setHint("Количество сигарет");
        etInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        new AlertDialog.Builder(this)
                .setTitle("Сколько сигарет?")
                .setView(countView)
                .setPositiveButton("Готово", (dialog, which) -> {
                    try {
                        int count = Integer.parseInt(etInput.getText().toString());
                        completeSmokingSession(minutes, purpose, count);
                    } catch (NumberFormatException e) {
                        completeSmokingSession(minutes, purpose, 1);
                    }
                })
                .setNegativeButton("Одну", (dialog, which) -> {
                    completeSmokingSession(minutes, purpose, 1);
                })
                .show();
    }

    // Обновленный метод завершения сессии
    private void completeSmokingSession(int minutes, String purpose, int cigaretteCount) {
        dailyCigaretteCount += cigaretteCount;
        dailySmokingTime += minutes;

        // Сохраняем детали сессии
        String timestamp = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date());
        String session = timestamp + " - " + minutes + " мин. (" + purpose + ")";
        if (cigaretteCount > 1) {
            session += " [" + cigaretteCount + " шт.]";
        }

        smokingSessions.add(session);
        recalculateAllTimeStats();
        saveAllData();
        updateStatsDisplay();

        // Показываем подтверждение
        String message = "Добавлено: " + minutes + " минут\n" +
                "Сигарет: " + cigaretteCount + "\n" +
                "Причина: " + purpose;
        new AlertDialog.Builder(this)
                .setTitle("Записано!")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    // Старый метод для совместимости
    private void completeSmokingSession(int minutes, String purpose) {
        completeSmokingSession(minutes, purpose, 1);
    }

    // Показ истории с возможностью удаления
    private void showHistory() {
        if (smokingSessions.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("История")
                    .setMessage("Пока нет записей о курении")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // Создаем адаптер с длинным нажатием для удаления
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, smokingSessions) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                view.setOnLongClickListener(v -> {
                    showDeleteSessionDialog(position);
                    return true;
                });
                return view;
            }
        };

        new AlertDialog.Builder(this)
                .setTitle("История (" + smokingSessions.size() + " сессий)")
                .setAdapter(adapter, (dialog, which) -> {
                    // Короткое нажатие - просмотр деталей
                    showSessionDetails(which);
                })
                .setPositiveButton("OK", null)
                .show();
    }

    // Диалог удаления сессии
    private void showDeleteSessionDialog(int position) {
        String sessionToDelete = smokingSessions.get(position);

        new AlertDialog.Builder(this)
                .setTitle("Удалить сессию?")
                .setMessage(sessionToDelete)
                .setPositiveButton("Удалить", (dialog, which) -> {
                    // Сохраняем данные сессии для пересчета
                    String session = smokingSessions.get(position);
                    int sessionCigarettes = 1;
                    int sessionMinutes = 0;

                    try {
                        // Извлекаем количество сигарет и минут из сессии
                        String timePart = session.split(" - ")[1];
                        String timeStr = timePart.split(" ")[0];
                        sessionMinutes = Integer.parseInt(timeStr);

                        // Проверяем количество сигарет
                        if (session.contains("[")) {
                            String countStr = session.split("\\[")[1].split(" ")[0];
                            sessionCigarettes = Integer.parseInt(countStr);
                        }
                    } catch (Exception e) {
                        // Если не удалось распарсить, используем значения по умолчанию
                        sessionCigarettes = 1;
                        sessionMinutes = 5;
                    }

                    // Удаляем сессию
                    smokingSessions.remove(position);

                    // Пересчитываем ВСЮ статистику заново
                    recalculateAllTimeStats();
                    recalculateDailyStats();

                    saveAllData();
                    updateStatsDisplay();

                    Toast.makeText(this, "Сессия удалена", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    // Детали сессии с возможностью редактирования
    private void showSessionDetails(int position) {
        String session = smokingSessions.get(position);

        new AlertDialog.Builder(this)
                .setTitle("Детали сессии")
                .setMessage(session + "\n\nНажмите и удерживайте для удаления")
                .setPositiveButton("OK", null)
                .show();
    }

    // Загрузка цены сигарет
    private void loadCigarettePrice() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        cigarettePackPrice = prefs.getFloat(KEY_PACK_PRICE, 0);

        if (cigarettePackPrice == 0) {
            showPriceSetupDialog();
        } else {
            cigarettePrice = cigarettePackPrice / 20; // 20 сигарет в пачке
        }
    }

    // Диалог настройки цены
    private void showPriceSetupDialog() {
        View priceView = getLayoutInflater().inflate(R.layout.dialog_input, null);
        EditText etInput = priceView.findViewById(R.id.etInput);
        etInput.setHint("Цена пачки в рублях");
        etInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);

        new AlertDialog.Builder(this)
                .setTitle("Настройка стоимости")
                .setMessage("Сколько стоит одна пачка сигарет?")
                .setView(priceView)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    try {
                        cigarettePackPrice = Float.parseFloat(etInput.getText().toString());
                        cigarettePrice = cigarettePackPrice / 20;
                        saveCigarettePrice();
                        updateStatsDisplay();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Введите корректную цену", Toast.LENGTH_SHORT).show();
                        showPriceSetupDialog();
                    }
                })
                .setCancelable(false)
                .show();
    }

    // Сохранение цены
    private void saveCigarettePrice() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(KEY_PACK_PRICE, cigarettePackPrice);
        editor.apply();
    }

    // Обновляем отображение статистики с стоимостью
    private void updateStatsDisplay() {
        float todayCost = dailyCigaretteCount * cigarettePrice;
        float weekCost = getWeekStats().count * cigarettePrice;
        float monthCost = getMonthStats().count * cigarettePrice;
        float allTimeCost = allTimeCigarettes * cigarettePrice;

        String stats = "Сегодня:\n" +
                "Сигарет: " + dailyCigaretteCount + "\n" +
                "Время: " + dailySmokingTime + "м\n" +
                "Стоимость: " + String.format("%.2f₽", todayCost) + "\n\n" +

                "За неделю:\n" +
                "Сигарет: " + getWeekStats().count + "\n" +
                "Время: " + getWeekStats().time + "м\n" +
                "Стоимость: " + String.format("%.2f₽", weekCost) + "\n\n" +

                "За месяц:\n" +
                "Сигарет: " + getMonthStats().count + "\n" +
                "Время: " + getMonthStats().time + "м\n" +
                "Стоимость: " + String.format("%.2f₽", monthCost) + "\n\n" +

                "Всего:\n" +
                "Сигарет: " + allTimeCigarettes + "\n" +
                "Время: " + allTimeSmokingTime + "м\n" +
                "Стоимость: " + String.format("%.2f₽", allTimeCost);

        tvStats.setText(stats);
    }

    // Загрузка всех данных
    private void loadAllData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastDate = prefs.getString(KEY_LAST_DATE, "");

        // Загружаем историю сессий
        Set<String> sessionsSet = prefs.getStringSet(KEY_ALL_SESSIONS, new HashSet<>());
        smokingSessions = new ArrayList<>(sessionsSet);

        // Загружаем список причин
        Set<String> purposesSet = prefs.getStringSet(KEY_PURPOSE_LIST, new HashSet<>());
        purposeList = new ArrayList<>(purposesSet);

        // Загружаем цену сигарет
        cigarettePackPrice = prefs.getFloat(KEY_PACK_PRICE, 0);
        if (cigarettePackPrice > 0) {
            cigarettePrice = cigarettePackPrice / 20;
        }

        // ВСЕГДА пересчитываем статистику из истории
        recalculateAllTimeStats();

        // Проверяем, нужно ли сбросить дневную статистику
        if (!lastDate.equals(today)) {
            // Новый день - сбрасываем дневную статистику
            dailyCigaretteCount = 0;
            dailySmokingTime = 0;
            recalculateDailyStats(); // Но пересчитываем на случай сессий с сегодняшней датой
        } else {
            // Тот же день - загружаем сохраненные значения
            dailyCigaretteCount = prefs.getInt(KEY_DAILY_COUNT, 0);
            dailySmokingTime = prefs.getLong(KEY_DAILY_TIME, 0);
        }

        // Если цена не установлена, показываем диалог
        if (cigarettePackPrice == 0) {
            showPriceSetupDialog();
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
            String fileName = "smoking_stats_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".txt";

            String content = generateExportContent();

            // Создаем файл во внешнем хранилище
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadsDir, fileName);

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content.getBytes());
            fos.close();

            Toast.makeText(this, "Файл сохранен в Загрузки: " + fileName, Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "Ошибка экспорта: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String generateExportContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== СТАТИСТИКА КУРЕНИЯ ===\n\n");

        float totalCost = allTimeCigarettes * cigarettePrice;
        float weekCost = getWeekStats().count * cigarettePrice;
        float monthCost = getMonthStats().count * cigarettePrice;

        sb.append("ОБЩАЯ СТАТИСТИКА:\n");
        sb.append("Всего сигарет: ").append(allTimeCigarettes).append("\n");
        sb.append("Общее время: ").append(allTimeSmokingTime).append(" минут\n");
        sb.append("Потрачено денег: ").append(String.format("%.2f₽", totalCost)).append("\n");
        sb.append("Среднее в день: ").append(allTimeCigarettes > 0 ?
                String.format("%.1f", (float) allTimeCigarettes / smokingSessions.size()) : 0).append(" сигарет\n\n");

        WeekStats weekStats = getWeekStats();
        sb.append("ЗА НЕДЕЛЮ:\n");
        sb.append("Сигарет: ").append(weekStats.count).append("\n");
        sb.append("Время: ").append(weekStats.time).append(" минут\n");
        sb.append("Потрачено: ").append(String.format("%.2f₽", weekCost)).append("\n\n");

        MonthStats monthStats = getMonthStats();
        sb.append("ЗА МЕСЯЦ:\n");
        sb.append("Сигарет: ").append(monthStats.count).append("\n");
        sb.append("Время: ").append(monthStats.time).append(" минут\n");
        sb.append("Потрачено: ").append(String.format("%.2f₽", monthCost)).append("\n\n");

        sb.append("ПОСЛЕДНИЕ СЕССИИ:\n");
        int count = 0;
        for (int i = smokingSessions.size() - 1; i >= 0 && count < 50; i--, count++) {
            sb.append(smokingSessions.get(i)).append("\n");
        }

        sb.append("\nЭкспортировано: ").append(
                new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date()));

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

    // Диалог импорта данных
    private void showImportDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Импорт данных")
                .setMessage("Это перезапишет все текущие данные. Продолжить?")
                .setPositiveButton("Импорт из файла", (dialog, which) -> {
                    importFromFile();
                })
                .setNeutralButton("Импорт из текста", (dialog, which) -> {
                    showTextImportDialog();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    // Полный пересчет всей статистики
    private void recalculateAllTimeStats() {
        allTimeCigarettes = 0;
        allTimeSmokingTime = 0;

        for (String session : smokingSessions) {
            try {
                int sessionCigarettes = 1;
                int sessionMinutes = 0;

                // Извлекаем время
                String timePart = session.split(" - ")[1];
                String timeStr = timePart.split(" ")[0];
                sessionMinutes = Integer.parseInt(timeStr);

                // Извлекаем количество сигарет (если указано)
                if (session.contains("[")) {
                    String countStr = session.split("\\[")[1].split(" ")[0];
                    sessionCigarettes = Integer.parseInt(countStr);
                }

                allTimeCigarettes += sessionCigarettes;
                allTimeSmokingTime += sessionMinutes;

            } catch (Exception e) {
                System.out.println("Ошибка пересчета сессии: " + session);
                // Добавляем минимальные значения для поврежденных данных
                allTimeCigarettes += 1;
                allTimeSmokingTime += 5;
            }
        }
    }

    // Статистика за неделю (исправленная)
    private WeekStats getWeekStats() {
        int weekCount = 0;
        long weekTime = 0;

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        Date weekAgo = calendar.getTime();

        for (String session : smokingSessions) {
            try {
                // Извлекаем дату и время из строки сессии
                String dateTimeStr = session.split(" - ")[0];
                Date sessionDate = sdf.parse(dateTimeStr);

                if (sessionDate != null && sessionDate.after(weekAgo)) {
                    // Извлекаем количество сигарет и время
                    int sessionCigarettes = 1;
                    int sessionMinutes = 0;

                    String timePart = session.split(" - ")[1];
                    String timeStr = timePart.split(" ")[0];
                    sessionMinutes = Integer.parseInt(timeStr);

                    if (session.contains("[")) {
                        String countStr = session.split("\\[")[1].split(" ")[0];
                        sessionCigarettes = Integer.parseInt(countStr);
                    }

                    weekCount += sessionCigarettes;
                    weekTime += sessionMinutes;
                }
            } catch (Exception e) {
                System.out.println("Ошибка расчета недельной статистики: " + session);
            }
        }

        return new WeekStats(weekCount, weekTime);
    }

    // Статистика за месяц (исправленная)
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
                    int sessionCigarettes = 1;
                    int sessionMinutes = 0;

                    String timePart = session.split(" - ")[1];
                    String timeStr = timePart.split(" ")[0];
                    sessionMinutes = Integer.parseInt(timeStr);

                    if (session.contains("[")) {
                        String countStr = session.split("\\[")[1].split(" ")[0];
                        sessionCigarettes = Integer.parseInt(countStr);
                    }

                    monthCount += sessionCigarettes;
                    monthTime += sessionMinutes;
                }
            } catch (Exception e) {
                System.out.println("Ошибка расчета месячной статистики: " + session);
            }
        }

        return new MonthStats(monthCount, monthTime);
    }

    // Импорт из текста
    private void showTextImportDialog() {
        View importView = getLayoutInflater().inflate(R.layout.dialog_input, null);
        EditText etInput = importView.findViewById(R.id.etInput);
        etInput.setHint("Вставьте данные экспорта сюда");
        etInput.setMinLines(10);

        new AlertDialog.Builder(this)
                .setTitle("Импорт данных")
                .setView(importView)
                .setPositiveButton("Импортировать", (dialog, which) -> {
                    String importText = etInput.getText().toString();
                    processImport(importText);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    // Обработка импортируемых данных
    private void processImport(String importText) {
        try {
            if (importText.contains("=== СТАТИСТИКА КУРЕНИЯ ===")) {
                // Парсим данные из формата экспорта
                parseExportFormat(importText);
                Toast.makeText(this, "Данные успешно импортированы!", Toast.LENGTH_LONG).show();
                updateStatsDisplay();
            } else {
                Toast.makeText(this, "Неверный формат данных", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка импорта: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Парсинг данных из формата экспорта (для данных в одной строке)
    private void parseExportFormat(String data) {
        smokingSessions.clear();

        System.out.println("Начало парсинга...");

        // Ищем раздел "ПОСЛЕДНИЕ СЕССИИ:" и извлекаем оттуда данные
        int sessionsStart = data.indexOf("ПОСЛЕДНИЕ СЕССИИ:");
        if (sessionsStart == -1) {
            Toast.makeText(this, "Раздел с сессиями не найден", Toast.LENGTH_LONG).show();
            return;
        }

        // Берем подстроку начиная с раздела сессий
        String sessionsPart = data.substring(sessionsStart + "ПОСЛЕДНИЕ СЕССИИ:".length());

        // Ищем конец раздела сессий (начало "Экспортировано:")
        int sessionsEnd = sessionsPart.indexOf("Экспортировано:");
        if (sessionsEnd != -1) {
            sessionsPart = sessionsPart.substring(0, sessionsEnd);
        }

        System.out.println("Раздел сессий: " + sessionsPart);

        // Ищем все сессии по шаблону даты
        Pattern pattern = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2} - \\d+ мин\\. \\([^)]+\\))");
        Matcher matcher = pattern.matcher(sessionsPart);

        int count = 0;
        while (matcher.find()) {
            String session = matcher.group(1).trim();
            smokingSessions.add(session);
            count++;
            System.out.println("Найдена сессия " + count + ": " + session);
        }

        if (count > 0) {
            recalculateAllTimeStats();
            recalculateDailyStats();
            saveAllData();
            Toast.makeText(this, "Импортировано " + count + " сессий", Toast.LENGTH_LONG).show();
            updateStatsDisplay();
        } else {
            Toast.makeText(this, "Сессии не найдены. Проверь формат данных.", Toast.LENGTH_LONG).show();
        }
    }

    // Пересчет дневной статистики
    private void recalculateDailyStats() {
        dailyCigaretteCount = 0;
        dailySmokingTime = 0;

        String today = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());

        for (String session : smokingSessions) {
            try {
                // Проверяем, сегодняшняя ли сессия
                String sessionDate = session.split(" ")[0];
                if (sessionDate.equals(today)) {
                    dailyCigaretteCount++;
                    String timePart = session.split(" - ")[1];
                    String timeStr = timePart.split(" ")[0];
                    dailySmokingTime += Integer.parseInt(timeStr);
                }
            } catch (Exception e) {
                System.out.println("Ошибка пересчета дневной статистики: " + session);
            }
        }
    }

    // Импорт из файла (упрощенная версия)
    private void importFromFile() {
        // Для простоты используем тот же диалог текстового импорта
        showTextImportDialog();
    }
}