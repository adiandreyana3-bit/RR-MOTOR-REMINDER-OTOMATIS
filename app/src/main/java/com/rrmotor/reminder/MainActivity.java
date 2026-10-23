package com.rrmotor.reminder;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.view.View;
import android.view.Gravity;
import android.widget.*;
import android.text.InputType;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private FirebaseFirestore db;

    private EditText nama, hp, motor, plat, km;
    private Spinner periode;
    private LinearLayout daftar;
    private TextView status;

    private final int merah = Color.rgb(183, 28, 28);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 20, 24, 24);
        root.setBackgroundColor(Color.rgb(245, 245, 245));
        scroll.addView(root);

        TextView judul = new TextView(this);
        judul.setText("RR MOTOR\nREMINDER GANTI OLI");
        judul.setTextSize(25);
        judul.setTextColor(Color.WHITE);
        judul.setGravity(Gravity.CENTER);
        judul.setPadding(16, 28, 16, 28);
        judul.setBackgroundColor(merah);
        root.addView(judul);

        TextView subjudul = new TextView(this);
        subjudul.setText("DATA PELANGGAN BARU");
        subjudul.setTextSize(18);
        subjudul.setTextColor(merah);
        subjudul.setPadding(0, 24, 0, 12);
        root.addView(subjudul);

        nama = input(root, "Nama pelanggan");
        hp = input(root, "Nomor WhatsApp");
        hp.setInputType(InputType.TYPE_CLASS_PHONE);

        motor = input(root, "Jenis motor");
        plat = input(root, "Nomor polisi");

        km = input(root, "KM terakhir (boleh kosong)");
        km.setInputType(InputType.TYPE_CLASS_NUMBER);

        TextView label = new TextView(this);
        label.setText("Jadwal pengingat");
        label.setTextColor(Color.DKGRAY);
        label.setTextSize(15);
        label.setPadding(0, 12, 0, 4);
        root.addView(label);

        periode = new Spinner(this);

        String[] pilihan = {
            "1 bulan",
            "1,5 bulan",
            "2 bulan"
        };

        ArrayAdapter<String> adapter =
            new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                pilihan
            );

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        );

        periode.setAdapter(adapter);
        root.addView(periode);

        Button simpan = tombol("SIMPAN PELANGGAN");
        root.addView(simpan);

        status = new TextView(this);
        status.setTextColor(Color.DKGRAY);
        status.setPadding(0, 12, 0, 12);
        root.addView(status);

        TextView judulDaftar = new TextView(this);
        judulDaftar.setText("DAFTAR PELANGGAN");
        judulDaftar.setTextSize(20);
        judulDaftar.setTextColor(merah);
        judulDaftar.setPadding(0, 24, 0, 12);
        root.addView(judulDaftar);

        daftar = new LinearLayout(this);
        daftar.setOrientation(LinearLayout.VERTICAL);
        root.addView(daftar);

        simpan.setOnClickListener(v -> simpanPelanggan());

        setContentView(scroll);

        muatPelanggan();
    }

    private EditText input(LinearLayout parent, String hint) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setHint(hint);
        e.setTextSize(16);
        e.setPadding(12, 8, 12, 8);
        parent.addView(e);
        return e;
    }

    private Button tombol(String teks) {
        Button b = new Button(this);
        b.setText(teks);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(merah)
        );
        return b;
    }

    private void simpanPelanggan() {

        String n = nama.getText().toString().trim();
        String h = hp.getText().toString().trim();
        String m = motor.getText().toString().trim();
        String p = plat.getText().toString().trim();
        String k = km.getText().toString().trim();

        if (n.isEmpty() || h.isEmpty()) {
            status.setText(
                "Nama dan nomor WhatsApp wajib diisi."
            );
            return;
        }

        if (!k.isEmpty()) {
            try {
                if (Long.parseLong(k) < 0) {
                    status.setText("KM tidak boleh negatif.");
                    return;
                }
            } catch (NumberFormatException e) {
                status.setText("Masukkan KM yang benar.");
                return;
            }
        }

        int hari;

        switch (periode.getSelectedItemPosition()) {
            case 1:
                hari = 45;
                break;
            case 2:
                hari = 60;
                break;
            default:
                hari = 30;
        }

        Calendar kalender = Calendar.getInstance();
        Date tanggalServis = kalender.getTime();

        kalender.add(Calendar.DAY_OF_MONTH, hari);
        Date tanggalPengingat = kalender.getTime();

        long kmBerikutnya = -1;

        if (!k.isEmpty()) {
            kmBerikutnya = Long.parseLong(k) + 1500;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("nama", n);
        data.put("whatsapp", h);
        data.put("motor", m);
        data.put("plat", p);
        data.put("kmTerakhir", k);
        data.put("kmBerikutnya", kmBerikutnya);
        data.put("periodeHari", hari);
        data.put("tanggalServis", tanggalServis);
        data.put("tanggalPengingat", tanggalPengingat);
        data.put("dibuatPada", new Date());

        status.setText("Menyimpan data...");

        db.collection("pelanggan")
            .add(data)
            .addOnSuccessListener(ref -> {
                status.setText("Pelanggan berhasil disimpan!");

                nama.setText("");
                hp.setText("");
                motor.setText("");
                plat.setText("");
                km.setText("");

                muatPelanggan();
            })
            .addOnFailureListener(e ->
                status.setText(
                    "Gagal menyimpan: " + e.getLocalizedMessage()
                )
            );
    }

    private void muatPelanggan() {

        db.collection("pelanggan")
            .get()
            .addOnSuccessListener(snapshot -> {

                daftar.removeAllViews();

                if (snapshot.isEmpty()) {
                    TextView kosong = new TextView(this);
                    kosong.setText("Belum ada pelanggan.");
                    daftar.addView(kosong);
                    return;
                }

                for (QueryDocumentSnapshot doc : snapshot) {

                    String n = doc.getString("nama");
                    String h = doc.getString("whatsapp");
                    String m = doc.getString("motor");
                    String p = doc.getString("plat");
                    String k = doc.getString("kmTerakhir");

                    Long kmNext = doc.getLong("kmBerikutnya");

                    Date due = doc.getDate("tanggalPengingat");

                    String tanggal = due == null
                        ? "-"
                        : new SimpleDateFormat(
                            "dd-MM-yyyy",
                            Locale.getDefault()
                        ).format(due);

                    TextView info = new TextView(this);

                    String teks =
                        "Pelanggan: " + aman(n) +
                        "\nWhatsApp: " + aman(h) +
                        "\nMotor: " + aman(m) +
                        "\nNomor polisi: " + aman(p) +
                        "\nKM terakhir: " +
                        (k == null || k.isEmpty() ? "Belum diisi" : k) +
                        "\nBatas maksimal KM berikutnya: " +
                        (kmNext == null || kmNext < 0
                            ? "Belum diisi"
                            : kmNext + " km") +
                        "\nJadwal pengingat: " + tanggal;

                    info.setText(teks);
                    info.setTextSize(15);
                    info.setTextColor(Color.DKGRAY);
                    info.setPadding(14, 14, 14, 14);
                    info.setBackgroundColor(Color.WHITE);

                    LinearLayout kartu = new LinearLayout(this);
                    kartu.setOrientation(LinearLayout.VERTICAL);
                    kartu.setPadding(0, 0, 0, 18);
                    kartu.addView(info);

                    Button wa = tombol("KIRIM PENGINGAT WHATSAPP");

                    wa.setOnClickListener(v ->
                        kirimWhatsApp(
                            n, h, due, k, kmNext
                        )
                    );

                    kartu.addView(wa);
                    daftar.addView(kartu);
                }
            })
            .addOnFailureListener(e -> {
                status.setText(
                    "Tidak bisa membaca Firebase. Periksa koneksi dan izin database."
                );
            });
    }

    private String aman(String teks) {
        return teks == null ? "-" : teks;
    }

    private void kirimWhatsApp(
        String namaPelanggan,
        String nomor,
        Date due,
        String kmTerakhir,
        Long kmNext
    ) {

        String sapaan =
            namaPelanggan == null || namaPelanggan.trim().isEmpty()
            ? "Bapak/Ibu"
            : namaPelanggan;

        String tanggal = due == null
            ? "-"
            : new SimpleDateFormat(
                "dd-MM-yyyy",
                Locale.getDefault()
            ).format(due);

        String pesan =
            "🏍️ *RR MOTOR*\n\n" +
            "Halo " + sapaan + ",\n\n" +
            "Kami mengingatkan bahwa sudah waktunya " +
            "melakukan pengecekan atau pergantian oli di RR MOTOR.\n\n" +
            "Tanggal pengingat: " + tanggal + "\n";

        if (kmNext != null && kmNext >= 0) {
            pesan += "\nBatas maksimal KM penggantian oli berikutnya: " +
                kmNext + " km.\n";
        }

        pesan +=
            "\nSilakan berkunjung ke RR MOTOR untuk " +
            "menjaga kondisi mesin motor Anda.\n\n" +
            "Terima kasih telah menjadi pelanggan RR MOTOR.";

        String no = nomor == null
            ? ""
            : nomor.replaceAll("[^0-9]", "");

        if (no.startsWith("0")) {
            no = "62" + no.substring(1);
        }

        if (no.isEmpty()) {
            Toast.makeText(
                this,
                "Nomor WhatsApp tidak tersedia.",
                Toast.LENGTH_SHORT
            ).show();
            return;
        }

        try {
            String url =
                "https://wa.me/" + no +
                "?text=" + Uri.encode(pesan);

            Intent intent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            );

            startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(
                this,
                "WhatsApp tidak dapat dibuka.",
                Toast.LENGTH_LONG
            ).show();
        }
    }
}
