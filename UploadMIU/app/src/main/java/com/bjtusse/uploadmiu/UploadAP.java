package com.bjtusse.uploadmiu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bjtusse.uploadmiu.entity.Ap;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UploadAP extends AppCompatActivity {

    TextView timeText;
    ArrayList<Ap> apList = new ArrayList<>();
    ListView apListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 获取 areaID
        Intent intent = getIntent();
        int data = intent.getIntExtra(Global.AREA_ID, -1);
        // 输出一下
        Toast.makeText(this, String.valueOf(data), Toast.LENGTH_SHORT).show();

        setContentView(R.layout.activity_upload_ap);


        if (ActivityCompat.checkSelfPermission(UploadAP.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(UploadAP.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);

        }

        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);
        wifiManager.startScan();

        List<ScanResult> scanResults = wifiManager.getScanResults();
        List<String> apStrlist = new ArrayList<>();

        int i = 0;

        timeText = (TextView) findViewById(R.id.time);
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.applyPattern("yyyy-MM-dd HH:mm:ss a");
        timeText.setText(sdf.format(new Date()));


        for (ScanResult scanResult : scanResults) {
            Ap ap = new Ap();
            ap.setSsid(scanResult.SSID);
            ap.setBssid(scanResult.BSSID);
            apList.add(ap);
            apStrlist.add("ssid："+scanResult.SSID + "  num:" + i);
            i++;
        }


        apListView = findViewById(R.id.ap_list);
        apListView.setAdapter(new ArrayAdapter<String>(UploadAP.this, android.R.layout.simple_expandable_list_item_1, apStrlist));






    }
}