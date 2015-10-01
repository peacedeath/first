package com.example.boss.first;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends /*ActionBar*/Activity implements View.OnClickListener{

    private static final String TAG = "MyLog";
    private static final Map<UUID, String> btServices = new HashMap<UUID, String>()/*{{
        put(UUID.fromString("00000000-0000-1000-8000-00805F9B34FB"), "Base");
        put(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"), "Serial Port");
    }}*/;
    ArrayList<Map<String, Object>> spData = new ArrayList<Map<String, Object>>();
    Button btn1, btn2, btn3;
    Spinner spDevices;
    TextView tv1;
    ImageView iv1;
    BluetoothAdapter btAdapter;
    BluetoothDevice btDevice;
    Set<BluetoothDevice> bondedDevices;

    ArrayAdapter<HashMap<String, String>> adapter;
    SimpleAdapter adapter2;
    private BluetoothSocket btSocket = null;
    private static String MacAdress = "20:13:04:23:07:20";
    boolean bt_state=false;
    private UUID MY_UUID;
    private ParcelUuid[] UUIDs;
    private ConnectedThred MyThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MapFromStringArray(R.array.my_string_array, btServices);
        for (Map.Entry<UUID, String> entry : btServices.entrySet()){
            Log.d(TAG, "*** entry.key = " + entry.getKey() + " entry.value = " + entry.getValue());
        }
        tv1 = (TextView) findViewById(R.id.tv1);
        tv1.setMovementMethod(new ScrollingMovementMethod());

        tv1.addTextChangedListener(new TextWatcher() {                                              // Добавляем нового слушателя событий для tv1
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}       // Вызывается перед тем как будут внесены изменения в TextView
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}          // Вызывается непосредственно после изменения в TextView. Тут нельзя манипулировать текстом
            @Override
            public void afterTextChanged(Editable s) {                                              // Вызывается после изменения в TextView. Тут можно манипулировать текстом
                if (tv1.getLineCount() * tv1.getLineHeight() > tv1.getHeight())                     // Если общее количество текста больше высоты поля, то
                    tv1.scrollTo(0, tv1.getLineCount() * tv1.getLineHeight() - tv1.getHeight());    // Прокрутить скролл до высоты текста минус высота поля. Таким образом новый текст будет внизу поля, а не вверху.
            }
        });
        iv1 = (ImageView) findViewById(R.id.iv1);
        spDevices = (Spinner) findViewById(R.id.spDevices);

        btn1 = (Button) findViewById(R.id.connect);
        btn2 = (Button) findViewById(R.id.get_srv);
        btn3 = (Button) findViewById(R.id.btn3);

        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);
        btn3.setOnClickListener(this);
        iv1.setOnClickListener(this);

        //adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        //adapter.add(spData);
        //adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter2 = new SimpleAdapter(this, spData, android.R.layout.simple_spinner_item, new String[]{"Name","Address"}, new int[]{android.R.id.text1, android.R.id.text2});
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDevices.setAdapter(adapter2);

        spDevices.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Map<String, Object> m = (Map<String, Object>)parent.getSelectedItem();
                MacAdress = m.get("Address").toString();
                Log.d(TAG, "*** Выбрано устройство " +  m.get("Name") + " MAC: " + MacAdress);
                MyError("Position = " + position + "\nName = " + m.get("Name") + "\nAddress = " + m.get("Address"));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            //tv1.setText("Bluetooth найден.");
            addText(tv1, "Bluetooth найден.", Color.rgb(0,255,0));
            Log.d(TAG, "Bluetooth найден.");
        } else {
            //tv1.setText("Bluetooth не найден! :(");
            addText(tv1, "Bluetooth не найден! :(", Color.rgb(0,255,0));
            Log.d(TAG, "Bluetooth не найден! :(");
        }
        if(!(bt_state = btAdapter.isEnabled())){
            iv1.setImageResource(R.drawable.bt_icon_off);
            tv1.append("\nBluetooth выключен");
            Log.d(TAG, "Bluetooth выключен");
            //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            iv1.setImageResource(R.drawable.bt_icon_on);
            tv1.append("\nBluetooth включен");
            Log.d(TAG, "Bluetooth включен");
            btAdapter.cancelDiscovery();                                                            // Отключаем видимость нашего устройства

//            Log.d(TAG, "***Пытаемся соединиться***");
//            btDevice = btAdapter.getRemoteDevice(MacAdress);                                        // Получаем удаленное устройство по его MAC адресу
//            tv1.append("\n***Получили device = " + btDevice.getName() + "***");
//            Log.d(TAG, "***Получили device = " + btDevice.getName() + "***");
//            UUIDs = btDevice.getUuids();

            bondedDevices = btAdapter.getBondedDevices();                                           // Получаем список спаренных устройств
            Map<String, Object> m;
            for (BluetoothDevice device : bondedDevices) {                                          // Идем по списку спаренных устройств и добавляем их в массив для выпадающего списка
                m = new HashMap<String, Object>();
                m.put("Name", device.getName());
                m.put("Address", device.getAddress());
                spData.add(m);
            }
            adapter2.notifyDataSetChanged();                                                        // Посылаем сигнал адаптеру выпадающего списка, что массив для списка изменился

//            if (UUIDs != null) {
//                tv1.append("\nУ \"" + btDevice.getName() + "\" найдены следующие сервисы:");
//                Log.d(TAG, "У \"" + btDevice.getName() + "\" найдены следующие сервисы:");
//                for (ParcelUuid pu : UUIDs) {
//                    Log.d(TAG, pu.getUuid().toString() + " = " + btServices.get(pu.getUuid()));
//                    try {
//                        if (btServices.get(pu.getUuid()).equalsIgnoreCase("SerialPort")) MY_UUID = pu.getUuid();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                    tv1.append("\n" + btServices.get(pu.getUuid()) + " (" + pu.toString() + ")");
//                }
//            }
//            try {
//                tv1.append("\nСоздание канала с UUID: " + MY_UUID);
//                Log.d(TAG, "Создание канала с UUID: " + MY_UUID);
//                btSocket = btDevice.createRfcommSocketToServiceRecord(MY_UUID);
//                tv1.append("\nКанал создан!");
//                Log.d(TAG, "Канал создан!");
//            } catch (IOException e) {
//                Log.d(TAG, "Ошибка при создании канала связи!", e);
//                tv1.append("\nОшибка при создании канала связи!");
//            }
//            try {
//                tv1.append("\nПопытка соедениться с устройством...");
//                Log.d(TAG, "Попытка соедениться с устройством...");
//                btSocket.connect();
//                tv1.append("\nСоединение установленно!");
//                Log.d(TAG, "Соединение установленно!");
//            } catch (IOException e) {
//                Log.d(TAG, "Ошибка при соединении с устройством!", e);
//                tv1.append("\nОшибка при соединении с устройством!");
//                try {
//                    Log.d(TAG, "Закрываем канал");
//                    tv1.append("\nЗакрываем канал");
//                    btSocket.close();
//                } catch (IOException e1) {
//                    Log.d(TAG, "Ошибка закрытия канала!", e1);
//                    tv1.append("\nОшибка закрытия канала!");
//                }
//            }
        }
    }
    /*@Override
    public void onResume() {
        super.onResume();
    }*/

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        menu.add("menu1");
//        menu.add("menu2");
//        menu.add("menu3");
//        menu.add("menu4");
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    @Override
    protected void onPause() {
        super.onPause();
        if(btSocket.isConnected())
        try {
            btSocket.close();
        } catch (IOException e) {
            Log.d(TAG, "Ошибка при закрытии сокета на onPause!", e);
            tv1.append("\nОшибка при закрытии сокета на onPause!");
        }
    }

    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
//        outState.put
    }

    protected void onRestoreInstanceState(Bundle saveInstanceState){
        super.onSaveInstanceState(saveInstanceState);
    }

//    private List<Map<String, ?>> CreateBTlist(HashMap<String, String> list){
//        List<Map<String, ?>> items = new ArrayList<Map<String, ?>>();
//
//        for (Map.Entry<String, String> entry : list.entrySet()){
//            Log.d(TAG, "*** entry.key = " + entry.getKey() + " entry.value = " + entry.getValue());
//            Map<String, String> map = new HashMap<String, String>();
//            map.put("Name", entry.getKey());
//            map.put("Address", entry.getValue());
//            items.add(map);
//        }
//        return items;
//    }

    private void MyError(String message){
        Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();
    }

    public void msbox(String mb_title, String mb_text) {
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(MainActivity.this);
        dlgAlert.setTitle(mb_title);
        dlgAlert.setMessage(mb_text);
        dlgAlert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connect:
                Log.d(TAG, "***Пытаемся соединиться***");
                btDevice = btAdapter.getRemoteDevice(MacAdress);                                        // Получаем удаленное устройство по его MAC адресу
                tv1.append("\n***Получили device = " + btDevice.getName() + "***");
                Log.d(TAG, "***Получили device = " + btDevice.getName() + "***");
                UUIDs = btDevice.getUuids();
                break;
            case R.id.get_srv:
                if (UUIDs != null) {
                    tv1.append("\nНайдены следующие сервисы:");
                    Log.d(TAG, "Найдены следующие сервисы:");
                    for (ParcelUuid pu : UUIDs) {
                        Log.d(TAG, pu.getUuid().toString() + " = " + btServices.get(pu.getUuid()));
                        try {
                            if (btServices.get(pu.getUuid()).equalsIgnoreCase("SerialPort")) MY_UUID = pu.getUuid();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        tv1.append("\n" + btServices.get(pu.getUuid()) + " (" + pu.toString() + ")");
                    }
                }
                break;
            case R.id.btn3:
                if (btSocket == null) {
                    if (MY_UUID != null) {
                        try {
                            tv1.append("\nСоздание канала с UUID: " + MY_UUID);
                            Log.d(TAG, "Создание канала с UUID: " + MY_UUID);
                            btSocket = btDevice.createRfcommSocketToServiceRecord(MY_UUID);
                            tv1.append("\nКанал создан!");
                            Log.d(TAG, "Канал создан!");
                        } catch (IOException e) {
                            Log.d(TAG, "Ошибка при создании канала связи!", e);
                            tv1.append("\nОшибка при создании канала связи!");
                        }
                        try {
                            tv1.append("\nПопытка соедениться с устройством...");
                            Log.d(TAG, "Попытка соедениться с устройством...");
                            btSocket.connect();
                            tv1.append("\nСоединение установленно!");
                            Log.d(TAG, "Соединение установленно!");
                            btn3.setText(R.string.ButtName3_off);
                        } catch (IOException e) {
                            Log.d(TAG, "Ошибка при соединении с устройством!", e);
                            tv1.append("\nОшибка при соединении с устройством!");
                            try {
                                Log.d(TAG, "Закрываем канал");
                                tv1.append("\nЗакрываем канал");
                                btSocket.close();
                            } catch (IOException e1) {
                                Log.d(TAG, "Ошибка закрытия канала!", e1);
                                tv1.append("\nОшибка закрытия канала!");
                            }
                        }
                    }
                } else {
                    try {
                        Log.d(TAG, "Закрываем канал");
                        tv1.append("\nЗакрываем канал");
                        btSocket.close();
                        btSocket = null;
                        btn3.setText(R.string.ButtName3_on);
                    } catch (IOException e1) {
                        Log.d(TAG, "Ошибка закрытия канала!", e1);
                        tv1.append("\nОшибка закрытия канала!");
                    }
                }
                break;
            case R.id.iv1:
                //this.msbox("Alert", iv1.getResources());
                if (!bt_state) {
                    iv1.setImageResource(R.drawable.bt_icon_on);
                    btAdapter.enable();
                    tv1.append("\nBluetooth включен");
                }
                else {
                    iv1.setImageResource(R.drawable.bt_icon_off);
                    btAdapter.disable();
                    tv1.append("\nBluetooth выключен");
                }
                bt_state = !bt_state;
                break;
        }

    }

    public void MapFromStringArray(int stringArrayResourceId, Map map) {

        //Map<UUID, String> m;
        String[] stringArray = getResources().getStringArray(stringArrayResourceId);
        SparseArray<String> outputArray = new SparseArray<>(stringArray.length);

        for (String entry : stringArray) {
            String[] splitResult = entry.split("\\|", 2);
            //m = new HashMap<UUID, String>();
            Log.d(TAG, "*** " + splitResult[0] + " : " + splitResult[1]);
            map.put(UUID.fromString(splitResult[0]), splitResult[1]);
        }
    }

    public void addText(TextView tv, String st, int color){
        final Spannable spannable = new SpannableString(st);
        //final ForegroundColorSpan style = new ForegroundColorSpan(color);
        spannable.setSpan(new ForegroundColorSpan(color), 0, st.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        //Spanned str = Html.fromHtml("<font color=\"" + color + "\">" + st + "</font>");
        tv.append(spannable);
        //setContentView(tv);
    }
    private class ConnectedThred extends Thread {
        private final BluetoothSocket copyBtSocket;
        private final OutputStream OutStrem;

        public ConnectedThred(BluetoothSocket socket){
            copyBtSocket = socket;
            OutputStream tmpOut = null;
            try{
                tmpOut = socket.getOutputStream();
            } catch (IOException e){}

            OutStrem = tmpOut;
        }

        public void sendData(String message) {
            byte[] msgBuffer = message.getBytes();
            Log.d(TAG, "***Отправляем данные: " + message + "***");

            try {
                OutStrem.write(msgBuffer);
            } catch (IOException e) {}
        }

        public void cancel(){
            try {
                copyBtSocket.close();
            }catch(IOException e){}
        }

        public Object status_OutStrem(){
            if (OutStrem == null){return null;
            }else{return OutStrem;}
        }
    }
}

