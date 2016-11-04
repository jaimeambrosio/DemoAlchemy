package alchemy.jj.demoalchemy;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ibm.watson.developer_cloud.alchemy.v1.AlchemyVision;
import com.ibm.watson.developer_cloud.alchemy.v1.model.ImageFace;
import com.ibm.watson.developer_cloud.alchemy.v1.model.ImageFaces;
import com.ibm.watson.developer_cloud.alchemy.v1.model.ImageKeyword;
import com.ibm.watson.developer_cloud.alchemy.v1.model.ImageKeywords;
import com.ibm.watson.developer_cloud.language_translation.v2.LanguageTranslation;
import com.ibm.watson.developer_cloud.language_translation.v2.model.Language;
import com.ibm.watson.developer_cloud.language_translation.v2.model.Translation;
import com.ibm.watson.developer_cloud.language_translation.v2.model.TranslationResult;
import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.AudioFormat;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.Voice;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;


public class MainActivity extends ActionBarActivity {

    private static int TOMAR_FOTO = 1;
    private static int SELECCIONAR_IMG = 2;

    private String rutaImagenes;
    private String rutaSonidos;
    private File directorioImagenes;
    private String nombreImagen;
    private String nombreSonido;
    private File fileImagenParaEnvio;
    private List<String> listStrKeyWords;
    private List<String> listStrFace;
    private Random random;

    private Button btnTomarFoto;
    private Button btnSeleccionarDeGale;
    private Button btnVolverAReproducir;
    private ImageView ivImagen;
    private GridView gvClass;
    private ListView lvRostros;
    private ProgressDialog progressDialog;
    private TextView txtEstRost;
    private TextView txtEstClass;
    private CheckBox cbHablar;
    private ProgressBar pbAudio;

    private AlchemyVision visionService;
    private LanguageTranslation translationService;
    private TextToSpeech textToSpeechService;
    private Voice[] arrVoiceES;


    private void inicializar() {

        listStrKeyWords = new ArrayList<>();
        listStrFace = new ArrayList<>();

        rutaImagenes = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/watson/";
        rutaSonidos = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC) + "/watson/";
        directorioImagenes = new File(rutaImagenes);
        directorioImagenes.mkdirs();
        new File(rutaSonidos).mkdirs();

        random = new Random();

        btnTomarFoto = (Button) findViewById(R.id.btnTomarFoto);
        btnTomarFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nombreImagen = "al_" + new Date().getTime() + ".jpg";
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                Uri uri = Uri.fromFile(new File(directorioImagenes, nombreImagen));
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                startActivityForResult(intent, TOMAR_FOTO);
            }
        });
        btnSeleccionarDeGale = (Button) findViewById(R.id.btnSeleccionarDeGale);
        btnSeleccionarDeGale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                startActivityForResult(intent, SELECCIONAR_IMG);
            }
        });
        btnVolverAReproducir = (Button) findViewById(R.id.btnVolverAReproducir);
        btnVolverAReproducir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (new File(rutaSonidos + nombreSonido).exists())
                        reproducirAudio(rutaSonidos + nombreSonido);
                } catch (Exception ex) {
                    Toast.makeText(MainActivity.this, ex.toString(), Toast.LENGTH_LONG).show();
                }

            }
        });
        ivImagen = (ImageView) findViewById(R.id.ivImagen);
        gvClass = (GridView) findViewById(R.id.gvClass);
        lvRostros = (ListView) findViewById(R.id.lvRostros);
        txtEstClass = (TextView) findViewById(R.id.txtEstClass);
        txtEstRost = (TextView) findViewById(R.id.txtEstRost);
        cbHablar = (CheckBox) findViewById(R.id.cbHablar);
        pbAudio = (ProgressBar) findViewById(R.id.pbAudio);
        pbAudio.setVisibility(ProgressBar.INVISIBLE);

        visionService = new AlchemyVision();
        visionService.setApiKey(getString(R.string.apyKeyVision));

        translationService = new LanguageTranslation();
        translationService.setUsernameAndPassword(getString(R.string.languajeTranslationUser), getString(R.string.languajeTranslationPass));

        textToSpeechService = new TextToSpeech();
        textToSpeechService.setUsernameAndPassword(getString(R.string.textToSpeechUser), getString(R.string.textToSpeechPass));

        arrVoiceES = new Voice[]{Voice.ES_ENRIQUE, Voice.ES_LAURA, Voice.ES_SOFIA};
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inicializar();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == this.RESULT_OK) {

            if (requestCode == TOMAR_FOTO) {
                fileImagenParaEnvio = new File(rutaImagenes + nombreImagen);
            } else if (requestCode == SELECCIONAR_IMG) {
                Uri selectedImage = data.getData();
                fileImagenParaEnvio = new File(getRealPathFromURI(selectedImage));
            }
            ivImagen.setImageBitmap(BitmapFactory.decodeFile(fileImagenParaEnvio.getAbsolutePath()));
            Toast.makeText(this, fileImagenParaEnvio.getAbsolutePath(), Toast.LENGTH_LONG).show();
            if (hayConexionInternet()) {
                TaskAlchemy taskAlchemy = new TaskAlchemy();
                taskAlchemy.execute(fileImagenParaEnvio);
                btnVolverAReproducir.setVisibility(Button.INVISIBLE);
            } else {
                showDialogModal("No se encuentra conectado a internet. Asegurese de tener una conexion activa.");
            }


        }
    }

    private class TaskAlchemy extends AsyncTask<File, Void, Object[]> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(MainActivity.this, "Espere un momento", "Analizando imagen...", true);
            progressDialog.setCancelable(false);
        }

        @Override
        protected Object[] doInBackground(File... params) {
            Object[] result = new Object[2];
            File f = params[0];

            if (f.exists()) {
                Boolean forceShowAll = true;
                Boolean knowledgeGraph = true;
                try {
                    ImageKeywords keywords = visionService.getImageKeywords(f, forceShowAll, knowledgeGraph).execute();
                    result[0] = keywords;
                } catch (Exception e) {
                    result[0] = "NO%%" + e.getMessage();
                }
                try {
                    long tam = f.length();
                    if (tam <= 1000000) {
                        ImageFaces faces = visionService.recognizeFaces(f, true).execute();
                        result[1] = faces;
                    } else {
                        result[1] = "NO%%No se pudo analizar, el maximo tamaño de imagen es 1MB";
                    }
                } catch (Exception e) {
                    result[1] = "NO%%" + e.getMessage();
                }

            }
            return result;
        }

        @Override
        protected void onPostExecute(Object[] objects) {
            super.onPostExecute(objects);
            TaskTranslation taskTranslation = new TaskTranslation();
            taskTranslation.execute(objects[0], objects[1]);
        }

    }

    private class TaskTranslation extends AsyncTask<Object, Void, Object[]> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            txtEstClass.setText("");
            txtEstRost.setText("");
        }

        @Override
        protected Object[] doInBackground(Object... params) {
            Object[] result = new Object[6];
            listStrKeyWords.clear();
            listStrFace.clear();

            //KEYWORDS
            boolean existsKeyWords = !params[0].toString().contains("NO%%");
            boolean existsFaces = !params[1].toString().contains("NO%%");
            ImageKeywords keywords = null;
            ImageFaces imageFaces = null;

            if (existsKeyWords) {
                keywords = (ImageKeywords) params[0];
                List<ImageKeyword> keywordsList = keywords.getImageKeywords();
                for (ImageKeyword key : keywordsList) {
                    listStrKeyWords.add(key.getText());
                }
            }
            if (existsFaces) {
                imageFaces = (ImageFaces) params[1];
                List<ImageFace> imageFaceList = imageFaces.getImageFaces();
                for (int i = 0; i < imageFaceList.size(); i++) {
                    ImageFace face = imageFaceList.get(i);
                    listStrFace.add("an " + face.getGender().getGender());
                }
            }
            List<String> listTemp = new ArrayList<>();
            listTemp.addAll(listStrKeyWords);
            listTemp.addAll(listStrFace);
            int i = 0;
            listStrFace.clear();
            /*---TRADUCCION--*/
            try {
                TranslationResult translationResult = translationService.translate(
                        listTemp,
                        Language.ENGLISH,
                        Language.SPANISH).execute();
                List<Translation> listTranslation = translationResult.getTranslations();
                for (Translation t : listTranslation) {
                    if (i < listStrKeyWords.size()) {
                        listStrKeyWords.set(i, t.getTranslation());
                        ++i;
                    } else {
                        listStrFace.add(t.getTranslation());
                    }
                }
            } catch (Exception ex) {

            }

            StringBuilder sb = new StringBuilder();
            sb.append("Hola,  el analisis de la imagen indica que contiene");
            if (existsKeyWords) {
                ArrayList<String> list = new ArrayList<>();
                List<ImageKeyword> keywordsList = keywords.getImageKeywords();
                i = 0;
                for (ImageKeyword key : keywordsList) {
                    list.add("Tipo");
                    list.add(listStrKeyWords.get(i));
                    list.add("Prob");
                    list.add(String.format("%.1f", key.getScore() * 100) + "%");
                    //sb.append(", con una probabilidad de " + String.format("%.1f", key.getScore() * 100) + "%, " + listStrKeyWords.get(i));
                    sb.append(" " + listStrKeyWords.get(i) + ", exactitud de " + String.format("%.1f", key.getScore() * 100) + "%, ");
                    ++i;
                }
                // sb.append(". ");
                //  LoadGridView(list, gvClass);
                result[0] = 0;
                result[1] = list;
            } else {
                String r = params[0].toString();
                sb.append(r.substring(4));
                result[0] = 1;
                result[1] = r.substring(4);
            }
            if (existsFaces) {
                ArrayList<String> list = new ArrayList<>();
                imageFaces = (ImageFaces) params[1];
                List<ImageFace> imageFaceList = imageFaces.getImageFaces();

                Bitmap bitmap = BitmapFactory.decodeFile(fileImagenParaEnvio.getAbsolutePath());
                Bitmap tempBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
                Canvas canvas = new Canvas(tempBitmap);
                canvas.drawBitmap(bitmap, 0, 0, null);
                //
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setTextSize(27);
                paint.setStrokeWidth(5);
                // Random r = new Random();
                if (imageFaceList.size() == 0) {
                    list.add("No se encontraron rostros.");
                    sb.append("No se encontraron rostros en la imagen.");
                    result[2] = 1;
                    result[3] = "No se encontraron rostros en la imagen.";
                } else {
                    sb.append("Se encontro " + imageFaceList.size());
                    if (imageFaceList.size() > 1) {
                        sb.append(" rostros.");
                    } else {
                        sb.append(" rostro.");
                    }
                    for (i = 0; i < imageFaceList.size(); i++) {
                        ImageFace face = imageFaceList.get(i);
                        String range = face.getAge().getAgeRange();

                        sb.append(" El rostro numero " + (i + 1) + " es " + listStrFace.get(i) + " cuya edad aproximada es ");
                        if (range.contains("-")) {
                            String[] anio = range.split("-");
                            sb.append("entre " + anio[0] + " y " + anio[1] + " años. ");
                        } else if (range.contains(">") || range.contains("<")) {
                            sb.append(range.charAt(0) + " " + range.substring(1) + " años. ");
                        } else {
                            sb.append(range + " años. ");
                        }

                        list.add("++ROSTRO " + (i + 1) + "++");
                        list.add("Genero: " + listStrFace.get(i) + "  - Prob: " + String.format("%.2f", face.getGender().getScore() * 100) + "%");
                        list.add("Edad: " + range + " - Prob: " + String.format("%.2f", face.getAge().getScore() * 100) + "%");
                        if (face.getIdentity() != null) {
                            list.add("Identidad: " + face.getIdentity().getName() + "  - Prob: " + String.format("%.2f", Double.valueOf(face.getIdentity().getScore()) * 100) + "%");
                            sb.append("Y probablemente sea " + face.getIdentity().getName() + ". ");
                        } else {
                            list.add("Rostro no identificado");
                        }

                        int x = face.getPositionX();
                        int y = face.getPositionY();
                        int h = face.getHeight();
                        int w = face.getWidth();

                        paint.setStyle(Paint.Style.STROKE);
                        int[] colors = {random.nextInt(256), random.nextInt(256), random.nextInt(256)};
                        paint.setARGB(255, colors[0], colors[1], colors[2]);
                        canvas.drawRect(x, y, x + w, y + h, paint);

                        paint.setStyle(Paint.Style.FILL_AND_STROKE);
                        canvas.drawText((i + 1) + "", x, y - 5, paint);

                    }

                    result[2] = 0;
                    result[3] = list;
                    result[4] = tempBitmap;
                }

            } else {
                String r = params[1].toString();
                sb.append("No se ha obtenido rostros en la imagen, " + r.substring(4));
                result[2] = 1;
                result[3] = r.substring(4);
            }

            result[5] = sb.append(" Hasta luego.").toString();
            return result;
        }

        @Override
        protected void onPostExecute(Object[] objects) {
            super.onPostExecute(objects);
            if (objects[0] != null && Integer.valueOf(objects[0].toString()) == 0) {
                LoadGridView((ArrayList<String>) objects[1], gvClass);
            } else {
                txtEstClass.setText(objects[1].toString());
                gvClass.setAdapter(null);
            }
            if (objects[2] != null && Integer.valueOf(objects[2].toString()) == 0) {
                LoadListView((ArrayList<String>) objects[3], lvRostros);
                ivImagen.setImageDrawable(new BitmapDrawable(getResources(), (Bitmap) objects[4]));
            } else {
                txtEstRost.setText(objects[3].toString());
                lvRostros.setAdapter(null);
            }

            if (cbHablar.isChecked()) {
                TaskTextToSpeech taskTextToSpeech = new TaskTextToSpeech();
                taskTextToSpeech.execute(objects[5].toString());
            }
            progressDialog.dismiss();
        }
    }

    private class TaskTextToSpeech extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            pbAudio.setVisibility(ProgressBar.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                publishProgress();

                nombreSonido = "so" + new Date().getTime() + ".wav";
                InputStream in = textToSpeechService.synthesize(params[0], arrVoiceES[random.nextInt(3)], AudioFormat.FLAC).execute();
                File file = new File(rutaSonidos + nombreSonido);
                writeToFile(in, file);
                reproducirAudio(rutaSonidos + nombreSonido);
                return true;
            } catch (Exception ex) {
                Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean b) {
            super.onPostExecute(b);
            pbAudio.setVisibility(ProgressBar.INVISIBLE);
            if (b)
                btnVolverAReproducir.setVisibility(Button.VISIBLE);
        }
    }

    private String getRealPathFromURI(Uri contentURI) {

        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            return contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            return cursor.getString(idx);
        }
    }

    private void LoadGridView(ArrayList<String> list, GridView gridView) {
        ArrayAdapter<String> ada = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list);
        gridView.setAdapter(ada);

        ListAdapter listAdapter = gridView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        int items = listAdapter.getCount();
        int rows = 0;

        View listItem = listAdapter.getView(0, null, gridView);
        listItem.measure(0, 0);
        totalHeight = listItem.getMeasuredHeight();
        int columns = 4;
        float x ;
        if (items > columns) {
            x = items / columns;
            rows = (int) (x + 1);
            totalHeight *= rows;
        }

        ViewGroup.LayoutParams params = gridView.getLayoutParams();
        params.height = totalHeight;
        gridView.setLayoutParams(params);
    }

    private void LoadListView(ArrayList<String> list, ListView listView) {
        ArrayAdapter<String> ada = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list);
        listView.setAdapter(ada);

        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    private void writeToFile(InputStream in, File file) throws Exception {
        OutputStream out = new FileOutputStream(file);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.close();
        in.close();
    }

    private void reproducirAudio(String s) throws Exception {
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setDataSource(s);
        mediaPlayer.prepare();
        mediaPlayer.start();
    }

    private boolean hayConexionInternet() {

        ConnectivityManager connectivityManager = (ConnectivityManager)
                getSystemService(this.CONNECTIVITY_SERVICE);

        NetworkInfo actNetInfo = connectivityManager.getActiveNetworkInfo();

        return (actNetInfo != null && actNetInfo.isConnected());
    }

    private void showDialogModal(String msj) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Información");
        builder.setMessage(msj);

        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        AlertDialog alert = builder.create();
        alert.show();

    }
}
