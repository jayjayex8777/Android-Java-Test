package com.example.objectselect2;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
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

        // RecyclerView 설정
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);

        // 데이터 생성 (1번부터 30번까지)
        List<String> dataList = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            dataList.add(String.valueOf(i));  // 숫자만 표시
        }

        // 어댑터 설정
        RectangleAdapter adapter = new RectangleAdapter(dataList);
        recyclerView.setAdapter(adapter);

        // ⭐ SnapHelper를 PagerSnapHelper로 변경 (더 빠르고 직관적인 중앙 정렬)
        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);
    }
}
