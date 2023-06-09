package com.android.captureinterface.service;

import static com.android.captureinterface.constant.ScreenShotConstant.DUMP_VIEW_TREE_TAG;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.android.captureinterface.concurrent.ChannelFactory;
import com.android.captureinterface.utils.DataUtils;
import com.android.captureinterface.utils.currentClickUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyAccessibilityService extends AccessibilityService {

    @Override
public void onAccessibilityEvent(AccessibilityEvent event) {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    executor.execute(new DumpViewTreeTask(event));
}

    class DumpViewTreeTask implements Runnable {

        private AccessibilityEvent event;

        public DumpViewTreeTask(AccessibilityEvent accessibilityEvent) {
            this.event = accessibilityEvent;
        }

        @Override
        public void run() {
            while (true) {
                boolean start = ChannelFactory.getStartDumpViewTree().receive();
                if (!start) {
                    break;
                }
                Log.d(DUMP_VIEW_TREE_TAG, "receive start dumpViewTree");
                // 获取当前活动窗口的根节点
                AccessibilityNodeInfo rootNode = getRootInActiveWindow();
//            traverseNode(rootNode);
//            List<AccessibilityWindowInfo> windows = getWindows();
//            for (AccessibilityWindowInfo window : windows) {
//                traverseNode(window.getRoot());
//            }
                if (rootNode != null) {
                    // 遍历整个控件树
                    try {
                        JSONObject jsonObject = accessibilityNodeInfoToJson(rootNode, 0);
                        Log.d("JSON", jsonObject.toString());
                        JsonToFIle(jsonObject);
                    } catch (JSONException e) {
                        Log.e("JSONException", e.toString());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                // 通知
                Log.d(DUMP_VIEW_TREE_TAG, "send end dumpViewTree");
                ChannelFactory.getEndDumpViewTree().send(true);
            }

        }
    }


    /**
     * 遍历应用程序的控件树
     *
     * @param node
     */
    private void traverseNode(AccessibilityNodeInfo node) {
        if (node == null) return;
        for (int i = 0; i < node.getChildCount(); i++) {
            try {
                AccessibilityNodeInfo childNode = node.getChild(i);
                String resourceId = childNode.getViewIdResourceName() == null ? "null" : childNode.getViewIdResourceName();
                CharSequence text = childNode.getText() == null ? "null" : childNode.getText();
                Log.d("childNode", "id = " + resourceId + " text = " + text);
                traverseNode(childNode);
            } catch (Exception e) {
                Log.e("traverseNode err", "traverseNode: ", e);
            }
        }
        node.recycle();
    }

    /**
     * 获取ActivityName
     *
     * @param event
     */
    private String getActivityName(AccessibilityEvent event) {
        String activityName = "";
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            AccessibilityNodeInfo source = event.getSource();
            if (source != null && source.getClassName() != null) {
                activityName = source.getClassName().toString();
                Log.d("ActivityName", "Current Activity: " + activityName);
            }
        }
        return activityName;
    }


    /**
     * JSONObject2File
     *
     * @param jsonObject
     * @throws IOException
     */
    public void JsonToFIle(JSONObject jsonObject) throws IOException {
        if(currentClickUtil.getClickFilePath() != null) {
            String json = jsonObject.toString();
            String fileName = "无障碍" + "_" + "TreeView(" + currentClickUtil.getInterfaceNum() + ").json";
            String strFilePath = currentClickUtil.getClickFilePath() + File.separator + fileName;
            File saveFile = new File(strFilePath);
            RandomAccessFile raf = new RandomAccessFile(saveFile, "rwd");
            raf.seek(saveFile.length());
            raf.write(json.getBytes());
            raf.close();
        }
    }

    /**
     * 将AccessibilityNodeInfo转化为Json
     *
     * @param node AccessibilityNodeInfo
     * @return JSONObject
     * @throws JSONException
     */
    public JSONObject accessibilityNodeInfoToJson(AccessibilityNodeInfo node, int index) throws JSONException {
        if (node == null) {
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("index", index);
        jsonObject.put("text", node.getText());
        String resourceId = node.getViewIdResourceName();

        if (resourceId != null) jsonObject.put("resource-id", resourceId);
        jsonObject.put("class", node.getClassName());
        jsonObject.put("package", node.getPackageName());
        jsonObject.put("content-desc", node.getContentDescription());
        jsonObject.put("checkable", node.isCheckable());
        jsonObject.put("checked", node.isChecked());
        jsonObject.put("clickable", node.isClickable());
        jsonObject.put("enabled", node.isEnabled());
        jsonObject.put("focusable", node.isFocusable());
        jsonObject.put("focused", node.isFocused());
        jsonObject.put("scrollable", node.isScrollable());
        jsonObject.put("long-clickable", node.isLongClickable());
        jsonObject.put("password", node.isPassword());
        jsonObject.put("selected", node.isSelected());
        Rect bounds = new Rect();
        node.getBoundsInParent(bounds);
        jsonObject.put("boundsInParent", bounds.toString().substring(4));
        node.getBoundsInScreen(bounds);
        jsonObject.put("boundsInScreen", bounds.toString().substring(4));
        JSONArray children = new JSONArray();
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            JSONObject childNodeJSONObj = accessibilityNodeInfoToJson(child, i);
            if (childNodeJSONObj != null) {
                children.put(childNodeJSONObj);
            }
            if (child != null) child.recycle();
        }
        if (children.length() > 0) {
            jsonObject.put("children", children);
        }
        return jsonObject;
    }

    @Override
    public void onInterrupt() {
    }


    /**
     * 注册AccessibilityServiceInfo
     */
    @Override
    protected void onServiceConnected() {
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        // 处理事件的类型
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        // 反馈的类型
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        // 处理事件的应用程序包名称
        info.packageNames = new String[]{DataUtils.getStr()};
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                | AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
                | AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY
                | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        info.notificationTimeout = 100;
        setServiceInfo(info);
    }


}


