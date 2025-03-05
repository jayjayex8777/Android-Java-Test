package com.example.objectselect1;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        // 기존의 LinearLayoutManager를 사용하지만 fling 속도를 높임
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);

        // 숫자 리스트 (1~10)
        List<Integer> numbers = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            numbers.add(i);
        }

        // 어댑터 설정
        RectangleAdapter adapter = new RectangleAdapter(this, numbers);
        recyclerView.setAdapter(adapter);

        // SnapHelper를 추가하여 한 번에 하나씩 스냅되도록 설정
        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

        // 🚀 Fling 속도 조절
        recyclerView.setOnFlingListener(null); // 기존 FlingListener 제거
        recyclerView.setOnFlingListener(new RecyclerView.OnFlingListener() {
            @Override
            public boolean onFling(int velocityX, int velocityY) {
                int newVelocityX = velocityX * 3; // 🚀 속도 3배 증가 (원하는 값으로 조정 가능)
                recyclerView.fling(newVelocityX, velocityY);
                return false;
            }
        });
    }
}
