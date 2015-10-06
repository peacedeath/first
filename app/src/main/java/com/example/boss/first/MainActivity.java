package com.example.boss.first;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.os.PersistableBundle;
import android.support.v4.view.WindowCompat;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends ActionBarActivity implements View.OnClickListener{

    private static final String LOG_TAG = "MyLog";
    private static final int MESSAGE_READ = 1;

    private static final Map<UUID, String> btServices = new HashMap<UUID, String>();
    static ArrayList<Map<String, Object>> spData = new ArrayList<Map<String, Object>>();
    Button btn1, btn2, btn3, btnSendCmd;
    static Spinner spDevices;
    TextView tv1;
    static BluetoothAdapter btAdapter;
    static BluetoothDevice btDevice;
    static InputStream InStream;
    Set<BluetoothDevice> bondedDevices;
    Handler h, hRx, hTx;
    Thread thRx, thTx;
    boolean ThreadExec;

    ArrayAdapter<HashMap<String, String>> adapter;
    private static SimpleAdapter spnAdapter = null;
    private static BluetoothSocket btSocket = null;
    private static String MacAdress = null;
    boolean bt_state=false;
    private static UUID MY_UUID;
    private static ParcelUuid[] UUIDs;
    private ConnectedThread MyThread = null;
    private String cmd;
//    private ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        Log.d(LOG_TAG, "onCreate!\n");
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 11) {
            requestFeature();
        }
        supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(false); //не показываем иконку приложения
        actionBar.setDisplayShowTitleEnabled(false); // и заголовок тоже прячем
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.my_actionbar);

        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) MapFromStringArray(R.array.my_string_array, btServices);
//            for (Map.Entry<UUID, String> entry : btServices.entrySet()) {
//                Log.d(LOG_TAG, "*** entry.key = " + entry.getKey() + " entry.value = " + entry.getValue());
//            }
        tv1 = (TextView) findViewById(R.id.tv1);

        btn1 = (Button) findViewById(R.id.connect);
        btn2 = (Button) findViewById(R.id.get_srv);
        btn3 = (Button) findViewById(R.id.btn3);
        btnSendCmd = (Button) findViewById(R.id.sendCmd);
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        spDevices = (Spinner) findViewById(R.id.sp_devices);

        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);
        btn3.setOnClickListener(this);
        btnSendCmd.setOnClickListener(this);
        findViewById(R.id.bt_sw).setOnClickListener(this);
        findViewById(R.id.overflow).setOnClickListener(this);

        tv1.setMovementMethod(new ScrollingMovementMethod());
        tv1.addTextChangedListener(new TextWatcher() {                                              // Добавляем нового слушателя событий для tv1
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }       // Вызывается перед тем как будут внесены изменения в TextView

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }          // Вызывается непосредственно после изменения в TextView. Тут нельзя манипулировать текстом

            @Override
            public void afterTextChanged(Editable s) {                                              // Вызывается после изменения в TextView. Тут можно манипулировать текстом
                if (tv1.getLineCount() * tv1.getLineHeight() > tv1.getHeight())                     // Если общее количество текста больше высоты поля, то
                    tv1.scrollTo(0, tv1.getLineCount() * tv1.getLineHeight() - tv1.getHeight());    // Прокрутить скролл до высоты текста минус высота поля. Таким образом новый текст будет внизу поля, а не вверху.
            }
        });
        ((ImageButton) findViewById(R.id.bt_sw)).setImageResource(btAdapter.isEnabled()?R.drawable.bt_icon_on_128:R.drawable.bt_icon_off_128);

        spnAdapter = new SimpleAdapter(this, spData, android.R.layout.simple_spinner_item, new String[]{"Name", "Address"}, new int[]{android.R.id.text1, android.R.id.text2});
        spnAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDevices.setAdapter(spnAdapter);
        if (btAdapter == null) addText(tv1, "*** Bluetooth адаптер не найден! ***\n", Color.RED);
        else spDevices.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(LOG_TAG, String.valueOf(parent.getSelectedItemPosition()));
                Map<String, Object> m = (Map<String, Object>) parent.getSelectedItem();
                MacAdress = m.get("Address").toString();
                Log.d(LOG_TAG, "*** Выбрано устройство " + m.get("Name") + " MAC: " + MacAdress);
                addText(tv1, "*** Пытаемся соединиться ***\n", Color.BLUE);
                btDevice = btAdapter.getRemoteDevice(MacAdress);                                        // Получаем удаленное устройство по его MAC адресу
                addText(tv1, "*** Соединились ***\n", Color.BLUE);
                addText(tv1, "*** Получили device = " + btDevice.getName() + " ***\n", Color.RED);
                UUIDs = btDevice.getUuids();                                                        // Получаем список сервисов
                for (ParcelUuid pu : UUIDs) {                                                       // Перебираем полученный список сервисов
                    try {
                        if (btServices.get(pu.getUuid()).equalsIgnoreCase("SerialPort"))            // Если находим сервис Serial Port
                            MY_UUID = pu.getUuid();                                                 //      сохраняем его UUID в MY_UUID
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                //                MyError("Position = " + position + "\nName = " + m.get("Name") + "\nAddress = " + m.get("Address"));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
               Log.d(LOG_TAG, String.valueOf(parent.getSelectedItemPosition()));
            }
        });

        if (savedInstanceState == null) {
            btAdapter = BluetoothAdapter.getDefaultAdapter();
            if (btAdapter == null) {
                addText(tv1, "Bluetooth не найден! :(\n", Color.RED);
            } else {
                addText(tv1, "Bluetooth найден.\n", Color.GREEN);
                if (!(bt_state = btAdapter.isEnabled())) {
                    addText(tv1, "Bluetooth выключен\n", Color.RED);
                    //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    //startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else {
                    addText(tv1, "Bluetooth включен\n", Color.GREEN);
                    btAdapter.cancelDiscovery();                                                    // Отключаем видимость нашего устройства

                    bondedDevices = btAdapter.getBondedDevices();                                   // Получаем список спаренных устройств
                    Map<String, Object> m;
                    for (BluetoothDevice device : bondedDevices) {                                  // Идем по списку спаренных устройств и добавляем их в массив для выпадающего списка
                        m = new HashMap<String, Object>();
                        m.put("Name", device.getName());
                        m.put("Address", device.getAddress());
                        spData.add(m);
                    }
//                    spnAdapter.notifyDataSetChanged();                                              // Посылаем сигнал адаптеру выпадающего списка, что массив для списка изменился
                }
            }
            hRx = new Handler() {
                public void handleMessage(android.os.Message msg) {
                    byte[] buff = (byte[])msg.obj;
                    String inpStr = new String(buff, 0, msg.what);
                    Log.d(LOG_TAG, String.valueOf(msg.what));
                    addText(tv1, inpStr, Color.MAGENTA);
                }
            };
        }
    }
    @Override
    public void onResume() {
        Log.d(LOG_TAG, "onResume!\n");
        super.onResume();
    }

    @Override
    protected void onStop() {
        Log.d(LOG_TAG, "onStop!\n");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "onDestroy!\n");
        if (btAdapter != null && btAdapter.isEnabled()) {
            btAdapter.disable();
            btAdapter = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        Log.d(LOG_TAG, "onPause!\n");
        super.onPause();
//        if (MyThread.status_OutStrem() != null) {
//            MyThread.cancel();
//        }
//        if(btSocket.isConnected())
//        try {
//            btSocket.close();
//        } catch (IOException e) {
//            Log.d(LOG_TAG, "Ошибка при закрытии сокета на onPause!", e);
//            tv1.append("\nОшибка при закрытии сокета на onPause!");
//        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        Log.d(LOG_TAG, "onSaveInstanceState!\n");
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putInt("spDevices", spDevices.getSelectedItemPosition());
        outState.putCharSequence("tv1", tv1.getText());
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState, PersistableBundle persistentState) {
        Log.d(LOG_TAG, "onRestoreInstanceState!\n");
        super.onRestoreInstanceState(savedInstanceState, persistentState);
        spDevices.setSelection(savedInstanceState.getInt("spDevices"));
        tv1.setText(savedInstanceState.getCharSequence("tv1"));
    }

/*    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(LOG_TAG, "onCreateOptionsMenu!\n");
        MainActivity.menu = menu;
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        final Spinner spinner = (Spinner) menu.findItem(R.id.sp_devices).getActionView();
        final Spinner spinner = (Spinner) findViewById(R.id.sp_devices);
        addText(tv1, String.valueOf(spnAdapter.isEmpty()) + "\n", Color.rgb(0xff,0x53,0x00));
        spinner.setAdapter(spnAdapter);
        if (btAdapter != null) {
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Map<String, Object> m = (Map<String, Object>) parent.getSelectedItem();
                    MacAdress = m.get("Address").toString();
                    Toast.makeText(getApplicationContext(), MacAdress, Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
        return true;
//        item1 = (MenuView.ItemView) findViewById(R.id.item1);
//        menu.findItem(R.id.item1).setIcon(getResources().getDrawable(R.drawable.bt_icon_off));
//        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d(LOG_TAG, "onPrepareOptionsMenu!\n");
//        menu.setGroupEnabled(R.id.bt_devices, btAdapter != null && btAdapter.isEnabled());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(LOG_TAG, "onOptionsItemSelected!\n");
        switch (item.getItemId()){
            case R.id.bt_sw:
                if (BT_SWITCH()) {
                    item.setIcon(R.drawable.bt_icon_on_128_tr);
                    menu.findItem(R.id.bt_devices).setEnabled(true);
                } else  {
                    item.setIcon(R.drawable.bt_icon_off_128_tr);
                    menu.findItem(R.id.bt_devices).setEnabled(false);
                    menu.findItem(R.id.bt_devices).setTitle(R.string.BT_DEVICES);
                }
                //invalidateOptionsMenu();
                break;
            case R.id.exit: finish(); break;
            default:
                if (item.getGroupId() == 0 && item.isCheckable()) item.setChecked(true);
                addText(tv1, String.valueOf(item.getGroupId()) + "\n", Color.RED);
                if(getObjByHash(spData, item.getItemId()) != null) {
                    MacAdress = getObjByHash(spData, item.getItemId()).get("Address").toString();
                    addText(tv1, MacAdress + "\n", Color.RED);
                    menu.findItem(R.id.bt_devices).setTitle(getObjByHash(spData, item.getItemId()).get("Name").toString());
                } else
                    addText(tv1, String.valueOf(item.getItemId()) + "\n", Color.RED);

//                invalidateOptionsMenu();
        }
        Toast.makeText(this, item.getTitle(), Toast.LENGTH_SHORT).show();
        return super.onOptionsItemSelected(item);
    }*/

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
                Log.d(LOG_TAG, "***Пытаемся соединиться***");
                btDevice = btAdapter.getRemoteDevice(MacAdress);                                        // Получаем удаленное устройство по его MAC адресу
                addText(tv1, "***Получили device = " + btDevice.getName() + "***\n", Color.rgb(255, 0, 0));
                UUIDs = btDevice.getUuids();
                for (ParcelUuid pu : UUIDs) {
                    try {
                        if (btServices.get(pu.getUuid()).equalsIgnoreCase("SerialPort")) MY_UUID = pu.getUuid();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            case R.id.get_srv:
                if (UUIDs != null) {
                    tv1.append("Найдены следующие сервисы:\n");
                    Log.d(LOG_TAG, "Найдены следующие сервисы:");
                    for (ParcelUuid pu : UUIDs) {
                        Log.d(LOG_TAG, pu.getUuid().toString() + " = " + btServices.get(pu.getUuid()));
                        tv1.append("\n" + btServices.get(pu.getUuid()) + " (" + pu.toString() + ")");
                    }
                }
                break;
            case R.id.btn3:
                if (btSocket == null) {
                    if (MY_UUID != null) {
                        addText(tv1, "\nСоздание канала с UUID: " + MY_UUID + "\n", Color.BLUE);
                        try {
                            btSocket = btDevice.createRfcommSocketToServiceRecord(MY_UUID);
                            addText(tv1, "Канал создан!\n", Color.BLUE);
                        } catch (IOException e) {
                            addText(tv1, "Ошибка при создании канала связи!\n", Color.RED, e);
                        }
                        addText(tv1, "Попытка соедениться с устройством...\n", Color.BLUE);
                        try {
                            btSocket.connect();
                            addText(tv1, "Соединение установленно!\n", Color.BLUE);
                            InStream = btSocket.getInputStream();
                            btn3.setText(R.string.ButtName3_off);
                        } catch (IOException e) {
                            addText(tv1, "Ошибка при соединении с устройством!\n", Color.RED, e);
                            try {
                                addText(tv1, "Закрываем канал\n", Color.BLACK);
                                btSocket.close();
                            } catch (IOException e1) {
                                addText(tv1, "Ошибка закрытия канала!\n", Color.RED, e);
                            }
                        }
                    } else if (!btAdapter.isEnabled()) {
                        addText(tv1, "Bluetooth выключен!\n", Color.RED);
                    }
                } else {
                    try {
                        tv1.append("Закрываем канал\n");
                        btSocket.close();
                        btSocket = null;
                        btn3.setText(R.string.ButtName3_on);
                    } catch (IOException e1) {
                        tv1.append("Ошибка закрытия канала!\n");
                    }
                }
                break;
            case R.id.sendCmd:
                AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
                dlgAlert.setTitle("Введите комманду");
                final EditText input = new EditText(this);
                dlgAlert.setView(input);

                dlgAlert.setPositiveButton("ОК", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        cmd = input.getText().toString();
//                        MyThread.sendData(cmd);
                        dialog.cancel();
                    }
                });
                dlgAlert.setCancelable(true);
                dlgAlert.create().show();
                break;
            case R.id.sp_devices:
                Toast.makeText(getApplicationContext(), v.getContentDescription(), Toast.LENGTH_SHORT).show();
                break;
            case R.id.bt_sw:
                Toast.makeText(getApplicationContext(), v.getContentDescription(), Toast.LENGTH_SHORT).show();
                if (BT_SWITCH()) {
                    ((ImageButton) v).setImageResource(R.drawable.bt_icon_on_128);
//                    ((ImageButton) findViewById(R.id.bt_devices)).setEnabled(true);
                } else  {
                    ((ImageButton) v).setImageResource(R.drawable.bt_icon_off_128);
//                    ((ImageButton) findViewById(R.id.bt_devices)).setEnabled(false);
                }
                break;
            case R.id.overflow:
                Toast.makeText(getApplicationContext(), v.getContentDescription(), Toast.LENGTH_SHORT).show();
                break;
        }

    }

    private boolean BT_SWITCH(){
        if (btAdapter == null) {
            addText(tv1, "Bluetooth не найден! :(\n", Color.RED);
            return false;
        }
        else {
            if (btAdapter.isEnabled()) {
                btAdapter.disable();                                                        // Выключаем БТ
                addText(tv1, "Bluetooth выключен\n", Color.RED);
                spData.clear();
//                spnAdapter.notifyDataSetChanged();
                return false;
            } else {
                btAdapter.enable();                                                         // Включаем БТ
                while (btAdapter.getState() != BluetoothAdapter.STATE_ON);
                addText(tv1, "Bluetooth включен\n", Color.GREEN);
                btAdapter.cancelDiscovery();                                                // Отключаем видимость нашего устройства
                if (spData.isEmpty()) {
                    bondedDevices = btAdapter.getBondedDevices();                               // Получаем список спаренных устройств
                    Map<String, Object> m;
//                    MenuItem btdev = menu.findItem(R.id.bt_devices);
//                    SubMenu sbdev = btdev.getSubMenu();
//                    sbdev.removeGroup(0);
                    for (BluetoothDevice device : bondedDevices) {                              // Идем по списку спаренных устройств и добавляем их в массив для выпадающего списка
                        m = new HashMap<String, Object>();
                        m.put("Name", device.getName());
                        m.put("Address", device.getAddress());
                        Log.d(LOG_TAG, m.toString());
                        spData.add(m);
//                        addText(tv1, device.getName() + ", " + String.valueOf(spData.get(spData.size()-1).hashCode()) + "\n", Color.RED);
//                        sbdev.add(0, spData.get(spData.size() - 1).hashCode(), 0, device.getName());
                    }
//                    sbdev.setGroupCheckable(0, true, true);
//                    spnAdapter.notifyDataSetChanged();                                          // Посылаем сигнал адаптеру выпадающего списка, что массив для списка изменился
                }
                return true;
            }
        }
    }
    private class ConnectedThread extends Thread {
        private final BluetoothAdapter btAdapter;
        private final BluetoothDevice btDevice;
        private final InputStream InStream;
        private final BluetoothSocket btSocket;


        private final OutputStream OutStrem;
        InputStream tmpIn = null;

        public ConnectedThread(BluetoothAdapter BA, String MAC){
            BluetoothSocket tmpBtSocket = null;
            OutputStream tmpOut = null;
            InputStream tmpIn = null;

            btAdapter = BA;
            addText(tv1, "Определение удаленного устройства HC-05...\n", Color.WHITE);
            btDevice = btAdapter.getRemoteDevice(MAC);
            if (btDevice != null) addText(tv1, "Устройство HC-05 определено.\n", Color.WHITE);
            addText(tv1, "Отключаем видимость нашего устройства.\n", Color.WHITE);

            addText(tv1, "Создание каналя связи...\n", Color.WHITE);
            try {
                tmpBtSocket = btDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            } catch (IOException e) {
                addText(tv1, "========> Не могу создать канал связи в BTConnect! <=======\n", Color.RED, e);
            }
            btSocket = tmpBtSocket;
            btAdapter.cancelDiscovery();
            addText(tv1, "Канал связи успешно создан!\nПодключение через канал связи...\n", Color.WHITE);
            try {
                btSocket.connect();
            } catch (IOException e) {
                addText(tv1, "========> Не могу подключиться через канал связи в BTConnect! <=======\n", Color.RED, e);
                close(btSocket);
            }
            addText(tv1, "Подключение успешно установленно!\nПолучение входящего потока\n", Color.WHITE);
            try {
                tmpIn = btSocket.getInputStream();
            } catch (IOException e) {
                addText(tv1, "========> Не могу получить входящий поток в BTConnect! <=======\n", Color.RED, e);
                close(btSocket);
            }
            InStream = tmpIn;
            addText(tv1, "Входной поток получен!\n", Color.WHITE);
            ThreadExec = true;

            try{
                tmpOut = btSocket.getOutputStream();
            } catch (IOException e){}

            OutStrem = tmpOut;
        }

        public void run(){
            byte[] buffer;
            int bytes, availableBytes = 0;

            while (ThreadExec) {
                try {
                    availableBytes = InStream.available();
                    if (availableBytes > 0) {
                        buffer = new byte[availableBytes];
                        bytes = InStream.read(buffer);
                        Log.d(LOG_TAG, "Доступно байт: " + String.valueOf(availableBytes) + " Считано байт: " + String.valueOf(bytes) + "\n");
                        hRx.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    } //else SystemClock.sleep(50);
                } catch (IOException e) {
                    break;
                }
            }
            cancel();
        }

        public void sendData(String message) {
            byte[] msgBuffer = message.getBytes();
            addText(tv1, "\n>> Послали: " + message + "\n", Color.CYAN);

            try {
                OutStrem.write(msgBuffer);
            } catch (IOException e) {
                addText(tv1, "Ошибка отпарвки данных!\n", Color.RED, e);
            }
        }

        public void cancel(){
            close(btSocket);
            close(InStream);
            if (btAdapter != null && btAdapter.isEnabled()) btAdapter.disable();
        }

        private void close(Closeable aConnectedObject) {
            if ( aConnectedObject == null ) return;
            try {
                aConnectedObject.close();
            } catch ( IOException e ) {
            }
        }
    }

    public void MapFromStringArray(int stringArrayResourceId, Map map) {
        //Map<UUID, String> m;
        String[] stringArray = getResources().getStringArray(stringArrayResourceId);
        SparseArray<String> outputArray = new SparseArray<String>(stringArray.length);

        for (String entry : stringArray) {
            String[] splitResult = entry.split("\\|", 2);
            //m = new HashMap<UUID, String>();
//            Log.d(LOG_TAG, "*** " + splitResult[0] + " : " + splitResult[1]);
            map.put(UUID.fromString(splitResult[0]), splitResult[1]);
        }
    }

    public void addText(TextView tv, String st, int color){
        final Spannable spannable = new SpannableString(st);
//        final ForegroundColorSpan style = new ForegroundColorSpan(color);
        spannable.setSpan(new ForegroundColorSpan(color), 0, st.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//        Spanned str = Html.fromHtml("<font color=\"" + color + "\">" + st + "</font>");
        tv.append(spannable);
        Log.d(LOG_TAG, st);
    }

    public void addText(TextView tv, String st, int color, IOException e) {
        final Spannable spannable = new SpannableString(st);
        spannable.setSpan(new ForegroundColorSpan(color), 0, st.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.append(spannable);
        Log.d(LOG_TAG, st, e);
    }

    public String DlgAlert() {
        final String[] out = new String[1];
        LayoutInflater li = LayoutInflater.from(MainActivity.this);
        View promptsView = li.inflate(R.layout.promt, null);                                // Получаем вид с файла prompt.xml, который применим для диалогового окна
        AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(MainActivity.this);    // Создаем AlertDialog
        mDialogBuilder.setView(promptsView);                                                // Настраиваем prompt.xml для нашего AlertDialog
        final EditText userInput = (EditText) promptsView.findViewById(R.id.input_text);    // Настраиваем отображение поля для ввода текста в открытом диалоге
        mDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                out[0] = userInput.getText().toString();
//                        dialog.cancel();
            }
        })
                .setCancelable(true);
        AlertDialog alertDialog = mDialogBuilder.create();                                  // Создаем AlertDialog
        alertDialog.show();
        return out[0];
    }

    public HashMap getObjByHash(ArrayList al, int hash){
        for (Object obj : al) {
            if (obj.hashCode() == hash) return (HashMap <String, Object>) obj;
        }
        return null;
    }

    private void requestFeature() {
        try {
            Field fieldImpl = ActionBarActivity.class.getDeclaredField("mImpl");
            fieldImpl.setAccessible(true);
            Object impl = fieldImpl.get(this);

            Class cls = Class.forName("android.support.v7.app.ActionBarActivityDelegate");

            Field fieldHasActionBar = cls.getDeclaredField("mHasActionBar");
            fieldHasActionBar.setAccessible(true);
            fieldHasActionBar.setBoolean(impl, true);

        } catch (NoSuchFieldException e) {
            Log.e(LOG_TAG, e.getLocalizedMessage(), e);
        } catch (IllegalAccessException e) {
            Log.e(LOG_TAG, e.getLocalizedMessage(), e);
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, e.getLocalizedMessage(), e);
        } catch (ClassNotFoundException e) {
            Log.e(LOG_TAG, e.getLocalizedMessage(), e);
        }
    }
}

