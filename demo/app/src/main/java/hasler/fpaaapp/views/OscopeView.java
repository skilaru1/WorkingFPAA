package hasler.fpaaapp.views;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.RequiresPermission;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import hasler.fpaaapp.R;
import hasler.fpaaapp.utils.Configuration;
import hasler.fpaaapp.utils.DriverFragment;
import hasler.fpaaapp.utils.ReadTimeOutException;
import hasler.fpaaapp.utils.Utils;
import hasler.fpaaapp.utils.targetProgram;

//import hasler.fpaaapp.utils.Configuration;

/*----------------------------------------------------------------------------------*/
/*----------------------------------MAIN_APP_VIEW-----------------------------------*/
/*----------------------------------------------------------------------------------*/
public class OscopeView extends DriverFragment {
    private final String TAG = "OscopeView";

    protected Handler mHandler = new Handler();

    //indicator of progress through current button
    protected ProgressBar progressBar;

    //graph for recorded data
    protected GraphView graph;

    //Alert to select wavFile in PlayButton
    protected AlertDialog.Builder builder;

    //name of selectedWavFile
    protected String selectedKey;

    //url of zip file with programming data
    protected String url = "URL not found";

    //protected String url = Configuration.DAC_ADC_WAV_LOCATION;
    //protected String selection;

    //location of unzipped wavfile
    protected String selectionPath;

    public static double[] linspace(double low, double high, int num) {
        double[] d = new double[num];
        for (int i = 0; i < num; i++) {
            d[i] = (high - low) * i / num + low;
        }
        return d;
    }

    public static long[] linspace(long low, long high, int num) {
        long[] l = new long[num];
        for (int i = 0; i < num; i++) {
            l[i] = (high - low) * i / num + low;
        }
        return l;
    }

    public static void plot(GraphView graph, long[][]... vals) {
        List<LineGraphSeries<DataPoint>> series = new ArrayList<>();
        for (long[][] val : vals) {
            DataPoint[] dataPoints = new DataPoint[val[0].length];
            for (int i = 0; i < val[0].length; i++) {
                dataPoints[i] = new DataPoint(val[0][i], val[1][i]);
            }

            LineGraphSeries<DataPoint> graphSeries = new LineGraphSeries<>(dataPoints);
            series.add(graphSeries);
        }
        plotSeries(graph, series);
    }

    public static void plot(GraphView graph, double[][]... vals) {
        List<LineGraphSeries<DataPoint>> series = new ArrayList<>();
        for (double[][] val : vals) {
            DataPoint[] dataPoints = new DataPoint[val[0].length];
            for (int i = 0; i < val[0].length; i++) {
                dataPoints[i] = new DataPoint(val[0][i], val[1][i]);
            }

            LineGraphSeries<DataPoint> graphSeries = new LineGraphSeries<>(dataPoints);
            series.add(graphSeries);
        }
        plotSeries(graph, series);
    }

    public static void plot(GraphView graph, double[] xvals, double[] yvals) {
        graph.removeAllSeries();
        DataPoint[] dataPoints = new DataPoint[xvals.length];
        for (int i = 0; i < xvals.length; i++) {
            dataPoints[i] = new DataPoint(xvals[i], yvals[i]);
        }
        LineGraphSeries<DataPoint> graphSeries = new LineGraphSeries<>(dataPoints);
        graph.addSeries(graphSeries);
    }
    //initialize View
    public static OscopeView newInstance() {
        return new OscopeView();
    }

    public OscopeView() { /* Required empty public constructor */ }

    //MediaPlayer to play wavFiles
    private MediaPlayer mediaPlayer;

    /*----------------------------------------------------------------------------------*/
    /*-------------------------------------METHODS--------------------------------------*/
    /*----------------------------------------------------------------------------------*/

    //removes the wav file from the zip file and stores it in a temporary file
    private void makeTempWaveFile(Map<String,byte[]> zipped_files, String selectedKey){
        byte[] wavFile = zipped_files.get(selectedKey);
        //selectionPath = new FileDescriptor();
        try{
            //initializes  file
            File unzippedWav = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/selection.wav");

            //writes wavFile byte array into new location
            FileOutputStream fos = new FileOutputStream(unzippedWav);
            fos.write(wavFile);
            fos.close();

            //location of wavFile
            selectionPath = unzippedWav.getAbsolutePath();
        }
        catch(IOException e){
            Utils.debugLine("makeTempWaveFile Exception: " + e.getMessage(),true);
        }

    }

    //wait for a bit to see if we can connect to the FPAA
    public boolean checkConnection(){
        try{
            if (!driver.connect()) {
                int count = 0;
                while(count < 10){
                    if(driver.connect()){
                        count = 10;
                    }
                    else {
                        count++;
                        Utils.debugLine("checkConnection count: " + count, true);
                    }
                }
                Utils.debugLine("not connected to driver", true);
                return false;
            }
        }
        catch(Exception e){
            Utils.debugLine("checkConnection (driver.connect) error: " + e.getMessage(),true);
            return false;
        }
        //Utils.debugLine("connected to driver",true);
        return true;
    }

    //displays data on graph
    //name[] is an array of names of the series i.e. {voltage, current, ...}
    //long[0][i] = x values, long[1][i] = y values
    public static void plot(GraphView graph, String name[], long[][]... vals) {
        //array of series
        List<LineGraphSeries<DataPoint>> seriesList = new ArrayList<>();
        int loopCounter = 0;
        for (long[][] val : vals) {
            DataPoint[] dataPoints = new DataPoint[val[0].length];
            for (int i = 0; i < val[0].length; i++) {
                dataPoints[i] = new DataPoint(val[0][i], val[1][i]);
            }
            //creates a new series to store dataPoints
            LineGraphSeries<DataPoint> graphSeries = new LineGraphSeries<>(dataPoints);
            //names this new series in legend
            graphSeries.setTitle(name[loopCounter++]);
            //adds this graphSeries to the list
            seriesList.add(graphSeries);

        }
        plotSeries(graph, seriesList);
    }

    //same as above, but double instead of long
    public static void plot(GraphView graph, String name[], double[][]... vals) {
        List<LineGraphSeries<DataPoint>> series = new ArrayList<>();
        int loopcounter = 0;
        for (double[][] val : vals) {
            DataPoint[] dataPoints = new DataPoint[val[0].length];
            for (int i = 0; i < val[0].length; i++) {
                dataPoints[i] = new DataPoint(val[0][i], val[1][i]);
            }

            LineGraphSeries<DataPoint> graphSeries = new LineGraphSeries<>(dataPoints);
            graphSeries.setTitle(name[loopcounter++]);
            series.add(graphSeries);
        }
        plotSeries(graph, series);
    }

    //clears graph, then plots a single series, (xvals,yvals), with title name
    public static void plot(GraphView graph, double[] xvals, double[] yvals, String name) {
        graph.removeAllSeries();
        DataPoint[] dataPoints = new DataPoint[xvals.length];
        for (int i = 0; i < xvals.length; i++) {
            dataPoints[i] = new DataPoint(xvals[i], yvals[i]);
        }
        LineGraphSeries<DataPoint> graphSeries = new LineGraphSeries<>(dataPoints);
        graphSeries.setTitle(name);
        graph.addSeries(graphSeries);
    }

    //order of the colors of series i.e. first series is blue, second is red......
    protected static final int[] COLORS = {Color.BLUE, Color.RED, Color.GREEN, Color.CYAN, Color.BLACK};

    //clears graph, then plots each graphSeries in seriesList
    public static void plotSeries(GraphView graph, List<LineGraphSeries<DataPoint>> seriesList) {
        graph.removeAllSeries();
        for (int i = 0; i < seriesList.size(); i++) {
            LineGraphSeries<DataPoint> series = seriesList.get(i);
            series.setColor(COLORS[i % COLORS.length]);
            graph.addSeries(series);
        }
    }

    //formats the graph according to a program design button press
    public static void formatGraphProgramDesign(GraphView graph, TextView graphTitle, String title){
        graphTitle.setText(title);

        graph.setTitle("Program Design");

        GridLabelRenderer glr = graph.getGridLabelRenderer();
        glr.setHorizontalAxisTitle("time (seconds)");

        LegendRenderer lr = graph.getLegendRenderer();
        lr.setVisible(true);
    }

    //formats the graph according to a getData button press
    public static void formatGraphGetData(GraphView graph){
        graph.setTitle("Get Data");

        GridLabelRenderer glr = graph.getGridLabelRenderer();
        glr.setHorizontalAxisTitle("time (seconds)");
        glr.setVerticalAxisTitle("Voltage (unknown unit)");

        LegendRenderer lr = graph.getLegendRenderer();
        lr.setVisible(true);
    }


    /*==========================================================================================
     onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    ----------------------------------------------------------------------------------------
    Description: picks out specific elf file from the zip file and writes the instruction enclosed
    to a particular address of the FPAA. Waits a certain amount of time for the FPAA to program
    properly
    ----------------------------------------------------------------------------------------
    Arguments:

    inflater: an object that helps turn your XML file into a View object so that the programmer can
    access visual elements from XML file

    container: holds the View, base class for Layout

    savedInstanceState: used to create a Fragment
    ----------------------------------------------------------------------------------------
    Result: returns a View. This basically means we update the tablet screen with new outputs
    ========================================================================================
    */

    /*----------------------------------------------------------------------------------*/
    /*---------------------------------------VIEW---------------------------------------*/
    /*----------------------------------------------------------------------------------*/
    //initialize the View (what people see)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) return null;
        super.onCreate(savedInstanceState);

        // Inflate the view XML file (translates the elements into what we see on the screen)
        final View view = inflater.inflate(R.layout.fragment_oscope_view, container, false);

        //visual elements from XML file: buttons are pressable, textview displays text(sometimes editable),
        // graph is a graph, alertdialog builder is used to create a window with various selectable options
        progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        final Button programDesignButton = (Button) view.findViewById(R.id.program_design_button);
        final Button getDataButton = (Button) view.findViewById(R.id.get_data_button);
        final Button playButton = (Button) view.findViewById(R.id.play_button);
        final Button pauseButton = (Button) view.findViewById(R.id.pause_button);
        final TextView graphTitle = (TextView) view.findViewById(R.id.textView2);
        //title wiped, will be set during each button press
        graphTitle.setText("Empty Title");
        graph = (GraphView) view.findViewById(R.id.graph);
        builder = new AlertDialog.Builder(getContext());


        //editable url input, used to grab zip file from github
        final EditText zipAddress = (EditText) view.findViewById(R.id.zip_file_location);
        final Button zipFileLocationButton = (Button) view.findViewById(R.id.zip_file_location_button);
        zipFileLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String addressString = zipAddress.getText().toString();
                Toast.makeText(parentContext, "Working!!! " + addressString, Toast.LENGTH_SHORT).show();
                url = addressString;
            }
        });

        /*----------------------------------------------------------------------------------*/
        /*------------------------------PROGRAM_DESIGN_TO_FPAA------------------------------*/
        /*----------------------------------------------------------------------------------*/
        //programs the zipFile specified in the zipAddress bar to the FPAA
        programDesignButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ThreadRunnable() {
                    @Override
                    protected void onPreExecute() {
                        getDataButton.setEnabled(false);
                        programDesignButton.setEnabled(false);
                        progressBar.setProgress(0);
                    }

                    @TargetApi(Build.VERSION_CODES.KITKAT)
                    @Override
                    public Boolean doInBackground(Void... params) {
                        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        File file = new File(path, "dac_adc.zip");
                        if (!download(file)) return null;

                        if (!driver.connect()) return null;

                        Map<String, byte[]> zipped_files = Utils.getZipContents(file.getAbsolutePath());
                        updateProgressBar(10);

                        if (!compileAndProgram(zipped_files, "tunnel_revtun_SWC_CAB.elf", 10 * 1000)) return null;
                        updateProgressBar(20);

                        if (!writeAscii(zipped_files, 0x7000, "switch_info")) return null;
                        if (!compileAndProgram(zipped_files, "switch_program.elf", 70 * 1000)) return null;
                        updateProgressBar(30);

                        if (!targetProgram(zipped_files)) return null;
                        updateProgressBar(70);

                        if (!writeAscii(zipped_files, 0x4300, "input_vector")) return null;
                        if (!writeAscii(zipped_files, 0x4200, "output_info")) return null;
                        if (!compileAndProgram(zipped_files, "voltage_meas.elf", 30 * 1000)) return null;
                        updateProgressBar(100);

                        // plot the switches
                        long[] d = Utils.toInts(2, driver.readMem(0x5000, 100));
                        List<long[]> sp = Utils.ffsplit(d);
                        if (sp.size() < 5) {
                            updateGraph(new long[][]{ linspace(1, d.length, d.length), d });
                        } else {
                            long[] a = sp.get(sp.size() - 4), b = sp.get(sp.size() - 3);
                            updateGraph(new long[][]{ linspace(1, a.length, a.length), a },
                                    new long[][]{ linspace(1, b.length, b.length), b });
                        }

                        return true;
                    }

                    boolean targetProgram(Map<String, byte[]> zipped_files) {
                        if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_mite")) return false; // checks out
                        if (!writeAscii(zipped_files, 0x6800, "pulse_width_table_mite")) return false; // checks out
                        if (!compileAndProgram(zipped_files, "recover_inject_aboveVt_CAB_mite.elf", 20 * 1000)) return false;

                        // coarse inj
                        if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_mite")) return false;
                        if (!compileAndProgram(zipped_files, "first_coarse_program_aboveVt_CAB_mite.elf", 20 * 1000)) return false;

                        // fine inj
                        if (!writeAscii(zipped_files, 0x7000, "target_info_aboveVt_mite")) return false;
                        if (!writeAscii(zipped_files, 0x6800, "Vd_table_30mV")) return false;
                        if (!compileAndProgram(zipped_files, "fine_program_aboveVt_m_ave_04_CAB_mite.elf", 20 * 1000)) return false;

                        // did all checks
                        return true;
                    }

                    @Override
                    protected void onPostExecute(Boolean result) {
                        super.onPostExecute(result);

                        if (result == null || !result) {
                            makeToastMessage("Error while trying to program the design");
                        }

                        progressBar.setProgress(100);
                        getDataButton.setEnabled(true);
                        programDesignButton.setEnabled(true);
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });

        /*----------------------------------------------------------------------------------*/
        /*--------------------------------GET_DATA_FROM_FPAA--------------------------------*/
        /*----------------------------------------------------------------------------------*/

        getDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ThreadRunnable() {
                    @Override
                    protected void onPreExecute() {
                        getDataButton.setEnabled(false);
                        programDesignButton.setEnabled(false);
                        progressBar.setProgress(0);
                    }

                    @TargetApi(Build.VERSION_CODES.KITKAT)
                    @Override
                    public Boolean doInBackground(Void... params) {
                        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        File file = new File(path, "dac_adc.zip");
                        if (!download(file)) return null;

                        if (!driver.connect()) return null;

                        Map<String, byte[]> zipped_files = Utils.getZipContents(file.getAbsolutePath());
                        updateProgressBar(10);

                        if (!writeAscii(zipped_files, 0x4300, "input_vector")) return null;
                        if (!writeAscii(zipped_files, 0x4200, "output_info")) return null;
                        updateProgressBar(30);

                        if (!driver.runWithoutData()) return null;
                        updateProgressBar(50);

                        driver.sleep(40 * 1000);
                        updateProgressBar(100);

                        long[] d = Utils.toInts(2, driver.readMem(0x6000, 100));
                        List<long[]> sp = Utils.ffsplit(d);

                        // plot the input vector beside the generated vector
                        byte[] data = Utils.parseHexAscii(zipped_files.get("input_vector"));
                        long[] l = Utils.toInts(2, Utils.swapBytes(data));
                        l = Utils.reverse(Arrays.copyOfRange(l, 3, l.length - 1));

                        // scaling
                        double[] n = new double[l.length];
                        for (int i = 0; i < l.length; i++) {
                            n[i] = l[i] * 2.5 / 30720.0;
                        }

                        // scaled input vector: n
                        // scaled output vector: b

                        if (sp.size() < 2) {
                            updateGraph(new long[][]{ linspace(1, d.length, d.length), d });
                        } else {
                            long[] a = sp.get(sp.size() - 1);
                            double[] b = new double[a.length];
                            for (int i = 0; i < a.length; i++) b[i] = -1*((a[i] - 5000) * 2.5 / 50.0); // b scales from a
                            updateGraph(new double[][]{ linspace(1.0, b.length, b.length), b }, // output
                                    new double[][]{ linspace(1.0, n.length, l.length), n }); // supposed input
                        }

                        return true;
                    }

                    @Override
                    protected void onPostExecute(Boolean result) {
                        super.onPostExecute(result);

                        if (result == null || !result) {
                            makeToastMessage("Error while trying to program the design");
                        }

                        progressBar.setProgress(100);
                        getDataButton.setEnabled(true);
                        programDesignButton.setEnabled(true);
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });
        /*----------------------------------------------------------------------------------*/
        /*-------------------------PLAY/FILTER_WAV_FILE_USING_FPAA--------------------------*/
        /*---------------------------(MUST_PROGRAM_DATA_FIRST)------------------------------*/
        /*----------------------------------------------------------------------------------*/
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ThreadRunnable() {
                    @Override
                    protected void onPreExecute() {
                        Utils.debugLine("PlayButton preExecution",false);
                        //Looper.prepare();
                        getDataButton.setEnabled(false);
                        programDesignButton.setEnabled(false);
                        playButton.setEnabled(false);
                        pauseButton.setEnabled(false);
                        progressBar.setProgress(0);
                        if(selectionPath != null){
                            File file = new File(selectionPath);
                            file.delete();
                        }
                        selectedKey = null;
                    }

                    @TargetApi(Build.VERSION_CODES.KITKAT)
                    @Override
                    public Boolean doInBackground(Void... params) {
                        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        if (url.equals("URL not found")) {
                            return false;
                        }
                        String title = url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("."));
                        File downloadedZip = new File(path, title.concat(".zip"));
                        makeToastMessage(title.concat(".zip"));
                        Utils.debugLine(title.concat(".zip"),true);
                        if(downloadedZip.exists()){downloadedZip.delete();}
                        if(downloadedZip.exists()){Utils.debugLine("file.delete not working", true);}
                        if (!download(downloadedZip)) {
                            Utils.debugLine("download = false", true);
                            makeToastMessage("download = false");
                            return false;
                        }
                        downloadedZip.deleteOnExit();

                        if (!checkConnection()) {
                            Utils.debugLine("driver not connected", true);
                            return false;
                        }
                        //Utils.debugLine("driver connected", true);
                        Map<String, byte[]> zipped_files = Utils.getZipContents(downloadedZip.getAbsolutePath());
                        updateProgressBar(10);
                        Set<String> keys = zipped_files.keySet();
                        Set<String> wavKeys = new HashSet<String>();
                        //Utils.debugLine("Sets Created",true);
                        for (String key : keys) {
                            if (key.substring(key.length() - 4).equals(".wav")) {
                                wavKeys.add(key);
                            }
                        }
                        if(downloadedZip.exists()){
                            Utils.debugLine("downloadedZip not deleted",true);
                        }
                        else{
                            //Utils.debugLine("downloadedZip deleted",true);
                        }
                        //Utils.debugLine("For loop key: keys finished",true);
                        while (wavKeys.size() == 0) {
                            makeToastMessage("no wav files in zip file");
                            Utils.debugLine("no wav files in zip file", true);

                            //Repeating initial process
                            downloadedZip.delete();

                            makeToastMessage(title.concat(".zip"));
                            Utils.debugLine(title.concat(".zip"),true);
                            if(downloadedZip.exists()){downloadedZip.delete();}
                            if(downloadedZip.exists()){Utils.debugLine("file.delete not working", true);}
                            if (!download(downloadedZip)) {
                                Utils.debugLine("download = false", true);
                                return false;
                            }
                            downloadedZip.deleteOnExit();

                            if (!checkConnection()) {
                                Utils.debugLine("driver not connected", true);
                                return false;
                            }
                            //Utils.debugLine("driver connected", true);
                            zipped_files = Utils.getZipContents(downloadedZip.getAbsolutePath());
                            updateProgressBar(10);
                            keys = zipped_files.keySet();
                            wavKeys = new HashSet<String>();
                            //Utils.debugLine("Sets Created",true);
                            for (String key : keys) {
                                if (key.substring(key.length() - 4).equals(".wav")) {
                                    wavKeys.add(key);
                                }
                            }
                            if(downloadedZip.exists()){
                                Utils.debugLine("downloadedZip not deleted",true);
                            }
                            else{
                                //Utils.debugLine("downloadedZip deleted",true);
                            }
                            //Utils.debugLine("For loop key: keys finished",true);

                        }

                        final CharSequence wavSequence[] = new CharSequence[wavKeys.size()];
                        int wavKeyCount = 0;
                        for (String wavKey : wavKeys) {
                            wavSequence[wavKeyCount] = wavKey;
                            wavKeyCount++;
                        }
                        //Utils.debugLine("For loop wavKey : wavKeys finished", true);
                        if(builder == null){Utils.debugLine("null builder", true);}
                        builder.setTitle("Select a .wav file");
                        //Utils.debugLine("builder title set", true);
                        //Utils.debugLine("wavSequence Length: " + wavSequence.length, true);

                        builder.setItems(wavSequence, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int select) {
                                SetSelectedKey(wavSequence[select].toString());
                            }
                        });
                        //Utils.debugLine("before create",true);
                        //AlertDialog alert = builder.create();
                        //alert.show();
                        makeAlertDialog(builder);
                        while(selectedKey == null){
                            //Utils.debugLine("waiting",true);
                        }
                        //Utils.debugLine("alert.show finished",true);
                        if (!writeAscii(zipped_files, 0x4300, "input_vector")) {
                            Utils.debugLine("writeAscii(input_vector) = false",true);
                            return false;
                        }
                        if (!writeAscii(zipped_files, 0x4200, "output_info")){
                            Utils.debugLine("writeAscii(output_info)",true);
                            return false;
                        }
                        updateProgressBar(30);

                        if (!driver.runWithoutData()){
                            Utils.debugLine("driver.runwithoutData() = false",true);
                            return false;
                        }
                        updateProgressBar(50);

                        //driver.sleep(40 * 1000);
                        updateProgressBar(100);
                        if (zipped_files.containsKey("voltage_meas.elf")) {
                            //Utils.debugLine("voltage_meas.elf exists", true);
                        } else {
                            Utils.debugLine("voltage_meas.elf DNE", true);
                        }
                        if (!compileAndProgram(zipped_files, "voltage_meas.elf", 20 * 1000)) {
                            Utils.debugLine("couldn't compile and program voltage_meas.elf", true);
                            return false;
                        }


                        //@TargetApi(Build.VERSION_CODES.KITKAT);
                       /* mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        try {
                            mediaPlayer.setDataSource(url);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        */
                        makeTempWaveFile(zipped_files, selectedKey);
                        //Utils.debugLine("selectionPath created",true);
                        makeMediaFile();
                        return true;
                    }

                    @Override
                    protected void onPostExecute(Boolean result) {
                        Utils.debugLine("PostExecute Start", true);
                        makeToastMessage("Done");

                        updateProgressBar(100);
                        programDesignButton.setEnabled(true);
                        getDataButton.setEnabled(true);
                        playButton.setEnabled(true);
                        pauseButton.setEnabled(true);

                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

        });

        /*----------------------------------------------------------------------------------*/
        /*---------------------------------PAUSE_WAV_FILE-----------------------------------*/
        /*----------------------------------------------------------------------------------*/
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "Pausing Sound ", Toast.LENGTH_SHORT).show();
                mediaPlayer.pause();
            }
        });
        //mediaPlayer.release();
        if(mediaPlayer != null) {
            mediaPlayer.release();
        }
        return view;
    }

    /*----------------------------------------------------------------------------------*/
    /*-------------------------METHODS_CALLED_USING_MHANDLER----------------------------*/
    /*------------------(USUALLY_CALLS_METHODS_FROM_METHODS_SECTION_ABOVE)--------------*/
    /*----------------------------------------------------------------------------------*/
    protected abstract class ThreadRunnable extends AsyncTask<Void, Void, Boolean> {
        public void updateProgressBar(final int i) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    progressBar.setProgress(i);
                }
            });
        }

        public void updateGraph(final long[][]... data) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    plot(graph, data);
                }
            });
        }

        public void updateGraph(final double[][]... data) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    plot(graph, data);
                }
            });
        }

        public void updateGraph(final double[] xdata, final double[] ydata) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    plot(graph, xdata, ydata);
                }
            });
        }

        public void makeToastMessage(final String text) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(parentContext, text, Toast.LENGTH_SHORT).show();
                }
            });
        }

        protected boolean compileAndProgram(Map<String, byte[]> loc, String name, int wait_ms) {
            byte[] data;

            if (!loc.containsKey(name)) {
                makeToastMessage("Zipped file does not contain file name \"" + name + "\"");
                return false;
            }

            try {
                data = Utils.compileElf(loc.get(name));
            } catch (Exception e) { return false; }

            if (!driver.programData(data)) return false;

            driver.sleep(wait_ms);
            return true;
        }


        protected boolean writeAscii(Map<String, byte[]> loc, int address, String name) {
            if (!loc.containsKey(name)) {
                makeToastMessage("Zipped file does not contain file name \"" + name + "\"");
                return false;
            }

            byte[] data = Utils.parseHexAscii(loc.get(name));
            data = Utils.swapBytes(data);
            boolean b = driver.writeMem(address, data);
            if (b) driver.sleep(1000);
            return b;
        }

        protected boolean download(File file) {
            // Download the file if it doesn't exist
            if (!file.exists()) {
                String url = Configuration.DAC_ADC_LOCATION;
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setTitle("DAC ADC");
                request.setDescription("Downloading the DAC ADC programming file");

                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "dac_adc.zip");

                DownloadManager manager = (DownloadManager) parentContext.getSystemService(Context.DOWNLOAD_SERVICE);
                manager.enqueue(request);

                // Check to see if the file downloaded
                int counter = 0, MAX_COUNTER = 100;
                while (counter <= MAX_COUNTER && !file.exists()) {
                    counter++;
                    driver.sleep(10);
                }
                if (counter > MAX_COUNTER) {
                    makeToastMessage("Downloading programming files failed (or is taking too long)");
                    return false;
                }
            }

            return true;
        }
    }

    public void SetSelectedKey(String selection){
        selectedKey = selection;
    }
    public void makeAlertDialog(final AlertDialog.Builder builder){
        mHandler.post(new Runnable(){
            @Override
            public void run() {
                AlertDialog alert = builder.create();
                alert.show();
            }
        });
    }
    public void makeMediaFile(){
        Utils.debugLine("makeMediaFile start",true);
        mHandler.post(new Runnable(){
            @Override
            public void run() {
                if(mediaPlayer != null){
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
                mediaPlayer = MediaPlayer.create(getActivity(), R.raw.heartbeat);
                try{
                    mediaPlayer.reset();
                    Utils.debugLine("MediaPlayer Reset",true);
                    mediaPlayer.setDataSource(selectionPath);
                    Utils.debugLine("MediaPlayer DataSource Set",true);
                    mediaPlayer.prepare();
                    Utils.debugLine("MediaPlayer Prepared",true);
                }
                catch(IOException e){
                    Utils.debugLine("mediaPlayerException: " + e.getMessage(),true);
                }
                makeToastMessage("Playing Sound");
                //Utils.debugLine("Playing Sound", true);
                mediaPlayer.start();//plays signal
            }
        });
    }

    public void makeToastMessage(final String text) {
        mHandler.post(new Runnable() {
            @Override
            public void run() { Toast.makeText(parentContext, text, Toast.LENGTH_SHORT).show(); }
        });
    }
}