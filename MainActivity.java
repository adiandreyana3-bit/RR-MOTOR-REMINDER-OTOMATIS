package com.rrmotor.remindergantioli;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import androidx.activity.ComponentActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends ComponentActivity {
    private EditText nameInput, phoneInput, bikeInput, dateInput, kmInput;
    private Spinner intervalSpinner;
    private LinearLayout customersLayout;
    private TextView statusText;
    private FirebaseFirestore db;
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("dd MMM yyyy", new Locale("id","ID"));
    private Calendar selectedDate = Calendar.getInstance();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try { FirebaseApp.initializeApp(this); } catch (Exception ignored) {}
        db = FirebaseFirestore.getInstance();
        createNotificationChannel();
        askNotificationPermission();
        buildUi();
        loadCustomers();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(18),dp(18),dp(24));
        root.setBackgroundColor(0xFFF5F5F5);
        scroll.addView(root);

        TextView title = new TextView(this); title.setText("🏍️ RR MOTOR"); title.setTextSize(26); title.setTextColor(0xFFB71C1C); title.setTypeface(null,1); root.addView(title);
        TextView subtitle = new TextView(this); subtitle.setText("REMINDER GANTI OLI"); subtitle.setTextSize(15); subtitle.setTextColor(0xFF333333); root.addView(subtitle);
        statusText = new TextView(this); statusText.setText("Menghubungkan ke Firebase..."); statusText.setPadding(0,dp(10),0,dp(10)); root.addView(statusText);

        TextView formTitle = label("Tambah pelanggan"); root.addView(formTitle);
        nameInput = field("Nama pelanggan *"); root.addView(nameInput);
        phoneInput = field("Nomor WhatsApp * (contoh 081234567890)"); nameInput.setSingleLine(true); root.addView(phoneInput);
        bikeInput = field("Jenis motor (opsional)"); root.addView(bikeInput);
        kmInput = field("KM terakhir (opsional)"); kmInput.setInputType(InputType.TYPE_CLASS_NUMBER); root.addView(kmInput);
        dateInput = field("Tanggal terakhir ganti oli"); dateInput.setFocusable(false); dateInput.setText(dateFmt.format(selectedDate.getTime())); dateInput.setOnClickListener(v -> showDatePicker()); root.addView(dateInput);

        intervalSpinner = new Spinner(this);
        String[] intervals = {"1 bulan","1,5 bulan","2 bulan"};
        intervalSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, intervals));
        root.addView(intervalSpinner);

        Button save = button("SIMPAN PELANGGAN"); root.addView(save);
        save.setOnClickListener(v -> saveCustomer());

        TextView dueTitle = label("Pelanggan jatuh tempo"); root.addView(dueTitle);
        customersLayout = new LinearLayout(this); customersLayout.setOrientation(LinearLayout.VERTICAL); root.addView(customersLayout);
        setContentView(scroll);
    }

    private TextView label(String s) { TextView t=new TextView(this); t.setText(s); t.setTextSize(18); t.setTextColor(0xFF222222); t.setTypeface(null,1); t.setPadding(0,dp(18),0,dp(8)); return t; }
    private EditText field(String hint) { EditText e=new EditText(this); e.setHint(hint); e.setSingleLine(true); e.setTextSize(15); e.setPadding(dp(12),dp(8),dp(12),dp(8)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52)); p.bottomMargin=dp(7); e.setLayoutParams(p); e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFB71C1C)); return e; }
    private Button button(String s) { Button b=new Button(this); b.setText(s); b.setTextColor(0xFFFFFFFF); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFB71C1C)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(8); p.bottomMargin=dp(8); b.setLayoutParams(p); return b; }
    private int dp(int n) { return (int)(n*getResources().getDisplayMetrics().density+0.5f); }

    private void showDatePicker() {
        new DatePickerDialog(this,(view,y,m,d)->{selectedDate.set(y,m,d,12,0,0); dateInput.setText(dateFmt.format(selectedDate.getTime()));},selectedDate.get(Calendar.YEAR),selectedDate.get(Calendar.MONTH),selectedDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveCustomer() {
        String name=nameInput.getText().toString().trim(), phone=phoneInput.getText().toString().trim();
        if(name.isEmpty() || phone.isEmpty()) { toast("Nama dan nomor WhatsApp wajib diisi."); return; }
        int interval=intervalSpinner.getSelectedItemPosition()==0?1:intervalSpinner.getSelectedItemPosition()==1?2:3;
        // Store interval as months in half-month units: 2=1 month, 3=1.5 months, 4=2 months.
        int halfMonths=intervalSpinner.getSelectedItemPosition()==0?2:intervalSpinner.getSelectedItemPosition()==1?3:4;
        Map<String,Object> data=new HashMap<>();
        data.put("name",name); data.put("phone",phone); data.put("bike",bikeInput.getText().toString().trim());
        data.put("lastOilDate",new Date(selectedDate.getTimeInMillis())); data.put("intervalHalfMonths",halfMonths);
        data.put("km",kmInput.getText().toString().trim()); data.put("reminded",false); data.put("createdAt",new Date());
        db.collection("customers").add(data).addOnSuccessListener(ref->{
            toast("Pelanggan tersimpan.");
            nameInput.setText(""); phoneInput.setText(""); bikeInput.setText(""); kmInput.setText("");
            loadCustomers();
        }).addOnFailureListener(e->toast("Gagal menyimpan Firebase: "+e.getLocalizedMessage()));
    }

    private Date dueDate(DocumentSnapshot d) {
        Date last=d.getDate("lastOilDate"); if(last==null)return new Date(0);
        Calendar c=Calendar.getInstance(); c.setTime(last);
        int half=d.getLong("intervalHalfMonths")==null?2:d.getLong("intervalHalfMonths").intValue();
        // 1 month=30 days; 1.5 months=45 days; 2 months=60 days, predictable due-date calculation.
        c.add(Calendar.DAY_OF_YEAR, half==2?30:half==3?45:60);
        return c.getTime();
    }

    private void loadCustomers() {
        db.collection("customers").get().addOnSuccessListener(snapshot->{
            customersLayout.removeAllViews(); int dueCount=0; Date now=new Date();
            for(DocumentSnapshot d:snapshot.getDocuments()) {
                Date due=dueDate(d); boolean dueNow=!due.after(now);
                if(!dueNow) continue;
                dueCount++;
                String name=d.getString("name"); String phone=d.getString("phone"); String bike=d.getString("bike");
                LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(12),dp(10),dp(12),dp(10)); card.setBackgroundColor(0xFFFFFFFF);
                LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2); cp.bottomMargin=dp(10); card.setLayoutParams(cp);
                TextView details=new TextView(this); details.setText((name==null?"Pelanggan":name)+"\\n"+(bike==null?"":bike)+"\\nJatuh tempo: "+dateFmt.format(due)); details.setTextSize(15); details.setTextColor(0xFF222222); card.addView(details);
                Button wa=button("KIRIM PESAN WHATSAPP"); card.addView(wa);
                wa.setOnClickListener(v->{
                    String normalized=normalizePhone(phone);
                    String message="🏍️ *RR MOTOR*\\n\\nHalo "+(name==null||name.trim().isEmpty()?"Bapak/Ibu":name)+", kami mengingatkan bahwa sudah waktunya melakukan pengecekan atau pergantian oli motor Anda.\\n\\nSilakan mampir ke RR MOTOR. Terima kasih.";
                    Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/"+normalized+"?text="+Uri.encode(message)));
                    try { startActivity(intent); d.getReference().update("reminded",true,"lastRemindedAt",new Date()); }
                    catch(Exception e) { toast("WhatsApp tidak ditemukan."); }
                });
                customersLayout.addView(card);
            }
            statusText.setText("Total pelanggan jatuh tempo: "+dueCount);
            if(dueCount==0){ TextView empty=new TextView(this); empty.setText("Belum ada pelanggan jatuh tempo."); empty.setTextColor(0xFF555555); empty.setPadding(0,dp(10),0,dp(10)); customersLayout.addView(empty); }
        }).addOnFailureListener(e->statusText.setText("Firebase belum tersambung: "+e.getLocalizedMessage()));
    }

    private String normalizePhone(String p) {
        if(p==null)return ""; String n=p.replaceAll("[^0-9]","");
        if(n.startsWith("0")) n="62"+n.substring(1);
        if(n.startsWith("8")) n="62"+n;
        return n;
    }

    private void createNotificationChannel() {
        if(Build.VERSION.SDK_INT>=26) {
            android.app.NotificationChannel channel=new android.app.NotificationChannel("rr_due","Pengingat Ganti Oli",android.app.NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifikasi pelanggan jatuh tempo ganti oli");
            getSystemService(android.app.NotificationManager.class).createNotificationChannel(channel);
        }
    }
    private void askNotificationPermission() {
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.POST_NOTIFICATIONS},101);
    }
    private void toast(String s) { Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }
}
