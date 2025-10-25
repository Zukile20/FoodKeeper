package com.example.foodkeeper.MealPlan;

import static com.example.foodkeeper.MealPlan.CalendarUtils.daysInMonthArray;
import static com.example.foodkeeper.MealPlan.CalendarUtils.monthYearFromDate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodkeeper.R;

import java.time.LocalDate;
import java.util.ArrayList;

public class MonthlyViewActivity extends AppCompatActivity implements CalendarAdapter.OnItemListener
{
    private TextView monthYearText;
    private RecyclerView calendarRecyclerView;
    private TextView okBtn;
    private TextView cancelBtn;
    private TextView yearTV;
    private TextView selectedDateTV;

    private void initWidgets()
    {
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView);
        monthYearText = findViewById(R.id.monthYearTV);
        yearTV = findViewById(R.id.yearTV);
        selectedDateTV = findViewById(R.id.selectedDateTV);
        okBtn = findViewById(R.id.okTextView);
        cancelBtn = findViewById(R.id.cancelBtn);
    }

    private void setMonthView()
    {
        yearTV.setText(String.valueOf(CalendarUtils.selectedDate.getYear()));

        String dayOfWeek = CalendarUtils.selectedDate.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault());
        String month = CalendarUtils.selectedDate.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault());
        String day = String.valueOf(CalendarUtils.selectedDate.getDayOfMonth());
        selectedDateTV.setText(dayOfWeek + ", " + month + " " + day);

        monthYearText.setText(monthYearFromDate(CalendarUtils.selectedDate));

        ArrayList<LocalDate> daysInMonth = daysInMonthArray(CalendarUtils.selectedDate);
        CalendarAdapter calendarAdapter = new CalendarAdapter(daysInMonth, this);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getApplicationContext(), 7);
        calendarRecyclerView.setLayoutManager(layoutManager);
        calendarRecyclerView.setAdapter(calendarAdapter);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_monthly_view);
        initWidgets();
        CalendarUtils.selectedDate = LocalDate.now();
        setMonthView();

        okBtn.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });

        cancelBtn.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }
    public void prevMonthAction(View view)
    {
        CalendarUtils.selectedDate = CalendarUtils.selectedDate.minusMonths(1);
        setMonthView();
    }

    public void nextMonthAction(View view)
    {
        CalendarUtils.selectedDate = CalendarUtils.selectedDate.plusMonths(1);
        setMonthView();
    }

    @Override
    public void onItemClick(int position, LocalDate date)
    {
        if(date != null)
        {
            CalendarUtils.selectedDate = date;
            setMonthView();
        }
    }

    public void weeklyAction(View view)
    {
        startActivity(new Intent(this, WeeklyViewActivity.class));
        finish();
        overridePendingTransition(R.anim.fade_out, 0);

    }

    public void closeMothlyView(View view) {
        weeklyAction(view);
    }
}