package PerfCheckLM;

import java.util.ArrayList;

public class Resource {
  public static final boolean VERBOSE = false;
  public static final ArrayList<String> targetCls = new ArrayList<String>(); 
  public static ArrayList<ExpensiveAPI> targetAPIs = new ArrayList<ExpensiveAPI>();
  
  static {
    targetCls.add("android.app.Activity");
    targetCls.add("android.widget.AbsListView$OnScrollListener");
    targetCls.add("android.widget.AdapterView$OnItemClickListener");
    targetCls.add("android.widget.AdapterView$OnItemLongClickListener");
    targetCls.add("android.widget.AdapterView$OnItemSelectedListener");
    targetCls.add("android.view.View$OnClickListener");
    targetCls.add("android.view.View$OnLongClickListener");
    targetCls.add("android.view.View$OnFocusChangeListener");
    targetCls.add("android.view.View$OnKeyListener");
    targetCls.add("android.view.View$OnTouchListener");
    targetCls.add("android.view.View$OnCreateContextMenuListener");
    targetCls.add("android.widget.CalendarView$OnDateChangeListener");
    targetCls.add("android.widget.AutoCompleteTextView$OnDismissListener");
    targetCls.add("android.widget.Chronometer$OnChronometerTickListener");
    targetCls.add("android.widget.CompoundButton$OnCheckedChangeListener");
    targetCls.add("android.widget.DatePicker$OnDateChangedListener");
    targetCls.add("android.widget.ExpandableListView$OnChildClickListener");
    targetCls.add("android.widget.ExpandableListView$OnGroupClickListener");
    targetCls.add("android.widget.ExpandableListView$OnGroupCollapseListener");
    targetCls.add("android.widget.ExpandableListView$OnGroupExpandListener");
    targetCls.add("android.widget.NumberPicker$OnScrollListener");
    targetCls.add("android.widget.NumberPicker$OnValueChangeListener");
    targetCls.add("android.widget.PopupMenu$OnDismissListener");
    targetCls.add("android.widget.PopupMenu$OnMenuItemClickListener");
    targetCls.add("android.widget.PopupWindow$OnDismissListener");
    targetCls.add("android.widget.RadioGroup$OnCheckedChangeListener");
    targetCls.add("android.widget.RatingBar$OnRatingBarChangeListener");
    targetCls.add("android.widget.SearchView$OnCloseListener");
    targetCls.add("android.widget.SearchView$OnQueryTextListener");
    targetCls.add("android.widget.SearchView$OnSuggestionListener");
    targetCls.add("android.widget.SeekBar$OnSeekBarChangeListener");
    targetCls.add("android.widget.ShareActionProvider$OnShareTargetSelectedListener");
    targetCls.add("android.widget.SlidingDrawer$OnDrawerCloseListener");
    targetCls.add("android.widget.SlidingDrawer$OnDrawerOpenListener");
    targetCls.add("android.widget.SlidingDrawer$OnDrawerScrollListener");
    targetCls.add("android.widget.TabHost$OnTabChangeListener");
    targetCls.add("android.widget.TextView$OnEditorActionListener");
    targetCls.add("android.widget.TimePicker$OnTimeChangedListener");
    targetCls.add("android.widget.ZoomButtonsController$OnZoomListener");
  
  //networking APIs
    targetAPIs.add(new ExpensiveAPI("java.net.URLConnection", "getContentLength"));
    targetAPIs.add(new ExpensiveAPI("java.net.URLConnection", "getContent"));
    targetAPIs.add(new ExpensiveAPI("java.net.URLConnection", "getContentEncoding"));
    targetAPIs.add(new ExpensiveAPI("java.net.URLConnection", "getOutputStream"));
    targetAPIs.add(new ExpensiveAPI("java.net.URLConnection", "getInputStream"));
    targetAPIs.add(new ExpensiveAPI("java.net.URL", "openStream"));
    targetAPIs.add(new ExpensiveAPI("java.net.URL", "openConnection"));
    targetAPIs.add(new ExpensiveAPI("java.net.URL", "getContent"));
    
    //database APIs
    targetAPIs.add(new ExpensiveAPI("android.database.sqlite.SQLiteDatabase", "queryWithFactory"));
    targetAPIs.add(new ExpensiveAPI("android.database.sqlite.SQLiteDatabase", "execSQL"));
    targetAPIs.add(new ExpensiveAPI("android.database.sqlite.SQLiteDatabase", "query"));
    targetAPIs.add(new ExpensiveAPI("android.database.sqlite.SQLiteDatabase", "rawQuery"));
    targetAPIs.add(new ExpensiveAPI("android.database.sqlite.SQLiteDatabase", "rawQueryWithFactory"));
    
    //file IO APIs
    targetAPIs.add(new ExpensiveAPI("android.context.ContextWrapper", "openFileInput"));
    targetAPIs.add(new ExpensiveAPI("android.context.ContextWrapper", "openFileOutput"));
    targetAPIs.add(new ExpensiveAPI("java.io.BufferedReader", "read"));
    targetAPIs.add(new ExpensiveAPI("java.io.BufferedReader", "readLine"));
    targetAPIs.add(new ExpensiveAPI("java.io.Writer", "append"));
    targetAPIs.add(new ExpensiveAPI("java.io.Writer", "write"));
    targetAPIs.add(new ExpensiveAPI("java.io.Reader", "read"));
    targetAPIs.add(new ExpensiveAPI("java.io.InputStreamReader", "read"));
    targetAPIs.add(new ExpensiveAPI("java.io.BufferedWriter", "write"));

    //bitmap related APIs
//    targetAPIs.add(new ExpensiveAPI("android.graphics.BitmapFactory", "<android.graphics.BitmapFactory: android.graphics.Bitmap decodeFile(java.lang.String,android.graphics.BitmapFactory$Options)>"));
//    targetAPIs.add(new ExpensiveAPI("android.graphics.Bitmap", "<android.graphics.Bitmap: boolean compress(android.graphics.Bitmap$CompressFormat,int,java.io.OutputStream)>"));
  
    //test
    //targetAPIs.add(new ExpensiveAPI("test", "com.example.inline.MyActivity.onCreate(Landroid/os/Bundle;)V"));
  }; 
}
