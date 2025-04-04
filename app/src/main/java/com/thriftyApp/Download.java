package com.thriftyApp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;

import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


public class Download extends AppCompatActivity {
    private static final String TAG = "PdfCreatorActivity";
    final private int REQUEST_CODE_ASK_PERMISSIONS = 111;
    private File pdfFile;
    ImageView imgdownload;
    DatabaseHelper databaseHelper;
    ArrayList<Transactions> list;
    Transactions transactions;
    Context context;
    String pdfname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download2);
        imgdownload = findViewById(R.id.downloadpdf);
        context = this;
        transactions = new Transactions ();
        databaseHelper =new DatabaseHelper (this);

        imgdownload.setOnClickListener(v -> {
            try {
                createPdfWrapper();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (DocumentException e) {
                e.printStackTrace();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

    }


    private void createPdfWrapper() throws IOException, DocumentException {
        // Check permissions for devices below Android 10
        int hasWriteStoragePermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasWriteStoragePermission != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_CONTACTS)) {
                    showMessageOKCancel("You need to allow access to Storage",
                            (dialog, which) -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_ASK_PERMISSIONS);
                                }
                            });
                    return;
                }
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_ASK_PERMISSIONS);
            }
        } else {
            // No need to check for storage permission on Android 10 and above
            createPdf(); // Call the method to create and save the PDF

            // Send a notification once the PDF is saved
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.download)
                    .setContentTitle(pdfname)
                    .setContentText("Download Completed.");

            Intent notificationIntent = new Intent(this, TransactionsActivity.class);

            // Create PendingIntent with FLAG_IMMUTABLE
            PendingIntent contentIntent = PendingIntent.getActivity(
                    this,
                    0,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE // Using FLAG_IMMUTABLE here
            );

            builder.setContentIntent(contentIntent);

            // Add the notification
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.notify(0, builder.build());

            // Start the next activity after the download
            Intent intent1 = new Intent(getApplicationContext(), TransactionsActivity.class);
            Toast.makeText(getApplicationContext(), "Report PDF downloaded successfully.", Toast.LENGTH_SHORT).show();
            startActivity(intent1);
            finish();
        }
    }


    private void createPdf() throws IOException, DocumentException {
        // For Android 10 and above (API level 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, pdfname);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS);

            // Insert a new row into the MediaStore
            ContentResolver resolver = getContentResolver();
            Uri pdfUri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues);

            if (pdfUri != null) {
                // Open an OutputStream to the newly created file
                OutputStream outputStream = resolver.openOutputStream(pdfUri);

                // Create a Document and PDF table as you did before
                Document document = new Document(PageSize.A4);
                PdfPTable table = new PdfPTable(new float[]{3, 3, 3, 3});
                table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
                table.getDefaultCell().setFixedHeight(50);
                table.setTotalWidth(PageSize.A4.getWidth());
                table.setWidthPercentage(100);
                table.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
                table.addCell("Transaction Tag");
                table.addCell("Amount in Euros");
                table.addCell("Transaction date");
                table.addCell("Income / Expense");
                table.setHeaderRows(1);

                list = databaseHelper.getTransactionsPDF();
                for (Transactions t : list) {
                    String tag = t.getTag();
                    long amnt = t.getAmount();
                    String dat = t.getCreated_at();

                    String exin = t.getExin() == 0 ? "Expense" : "Income";
                    Log.i("Preepdf", t.toString());
                    table.addCell(tag);
                    table.addCell(String.valueOf(amnt));
                    table.addCell(dat);
                    table.addCell(exin);
                }

                PdfWriter.getInstance(document, outputStream);
                document.open();
                Font f = new Font(Font.FontFamily.HELVETICA, 30.0f, Font.UNDERLINE, BaseColor.BLUE);
                document.add(new Paragraph(Utils.userName + "'s Transactions" + "\n\n", f));
                document.add(new Paragraph("Generated by MY BUDGET\n\n"));
                document.add(table);
                document.close();

                // Notify the media scanner about the new file
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, pdfUri));

            }
        } else {
            // For Android 9 and below, use your previous method (direct file saving)
            File docsFolder = new File(Environment.getExternalStorageDirectory() + "/Documents");
            if (!docsFolder.exists()) {
                docsFolder.mkdir();
                Log.i(TAG, "Created a new directory for PDF");
            }

            pdfname = "TransactionsReport" + Utils.pdfNumber + ".pdf";
            Utils.pdfNumber = Utils.pdfNumber + 1;
            pdfFile = new File(docsFolder.getAbsolutePath(), pdfname);
            OutputStream output = new FileOutputStream(pdfFile);
            Document document = new Document(PageSize.A4);
            PdfPTable table = new PdfPTable(new float[]{3, 3, 3, 3});
            table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
            table.getDefaultCell().setFixedHeight(50);
            table.setTotalWidth(PageSize.A4.getWidth());
            table.setWidthPercentage(100);
            table.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell("Transaction Tag");
            table.addCell("Amount in Euros");
            table.addCell("Transaction date");
            table.addCell("Income / Expense");
            table.setHeaderRows(1);

            list = databaseHelper.getTransactionsPDF();
            for (Transactions t : list) {
                String tag = t.getTag();
                long amnt = t.getAmount();
                String dat = t.getCreated_at();

                String exin = t.getExin() == 0 ? "Expense" : "Income";
                Log.i("Preepdf", t.toString());
                table.addCell(tag);
                table.addCell(String.valueOf(amnt));
                table.addCell(dat);
                table.addCell(exin);
            }

            PdfWriter.getInstance(document, output);
            document.open();
            Font f = new Font(Font.FontFamily.HELVETICA, 30.0f, Font.UNDERLINE, BaseColor.BLUE);
            document.add(new Paragraph(Utils.userName + "'s Transactions" + "\n\n", f));
            document.add(new Paragraph("Generated by MY BUDGET\n\n"));
            document.add(table);
            document.close();

        }
    }


    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }



    private void previewPdf() {
        PackageManager packageManager = context.getPackageManager();
        Intent testIntent = new Intent(Intent.ACTION_VIEW);
        testIntent.setType("application/pdf");
        List<ResolveInfo> list = packageManager.queryIntentActivities(testIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (!list.isEmpty()) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            Uri uri = Uri.fromFile(pdfFile);
            intent.setDataAndType(uri, "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "Download a PDF Viewer to see the generated PDF", Toast.LENGTH_SHORT).show();
        }

    }


}
