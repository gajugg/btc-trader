package org.chartsy.main.chartsy;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import javax.swing.BoundedRangeModel;
import javax.swing.JScrollBar;
import java.util.Timer;
import java.util.TimerTask;
import org.chartsy.main.chartsy.chart.AbstractChart;
import org.chartsy.main.dataset.Dataset;
import org.chartsy.main.managers.ChartFrameManager;
import org.chartsy.main.managers.ChartManager;
import org.chartsy.main.managers.DatasetManager;
import org.chartsy.main.managers.UpdaterManager;
import org.chartsy.main.updater.AbstractUpdater;
import org.chartsy.main.utils.Stock;
import org.chartsy.main.utils.XMLUtils;
import org.openide.windows.TopComponent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author viorel.gheba
 */
public class ChartFrame extends TopComponent implements AdjustmentListener, XMLUtils.ToXML {

    private Stock stock;
    private String chartName;
    private String time;
    private String id;
    private boolean forced = false;
    
    private AbstractChart chart;
    private ChartToolbar chartToolbar;
    private ChartPanel chartPanel;
    private ChartProperties chartProperties;
    private ChartRenderer chartRenderer;
    private JScrollBar horizontalBar;
    private Marker marker;
    private Timer timer;

    public ChartFrame() {}

    public ChartFrame(Stock stock, String chart) {
        this(stock, chart, null);
    }

    public ChartFrame(Stock s, String c, String displayName) {
        stock = s;
        chartName = c;
        setDisplayName(displayName == null ? stock.getKey() + " Chart" : displayName);
        initComponents();
    }

    public void initComponents() {
        setLayout(new BorderLayout());
        time = DatasetManager.DAILY;
        chart = ChartManager.getDefault().getChart(chartName);
        chartProperties = ChartProperties.newInstance();
        chartRenderer = ChartRenderer.newInstance(this);
        chartToolbar = ChartToolbar.newInstance(this);
        chartPanel = ChartPanel.newInstance(this);
        horizontalBar = initHorizontalScrollBar();
        marker = Marker.newInstance(this);
        timer = new Timer();
        timer.schedule(new PeriodTimer(), 5000, 5000);

        add(chartToolbar, BorderLayout.NORTH);
        add(chartPanel, BorderLayout.CENTER);
        add(horizontalBar, BorderLayout.SOUTH);
    }

    protected void componentClosed() { timer.cancel(); if (!forced) { ChartFrameManager.getDefault().removeChartFrame(id); } }
    protected void componentOpened() { repaint(); }

    public void setForced(boolean b) { forced = b; }
    public boolean isForced() { return forced; }
    /*public void setUndocked(boolean b) { docked = b; }*/

    public Stock getStock() { return stock; }
    public void setStock(Stock s) { stock = s; }

    public void setChart(String name) { chartName = name; chart = ChartManager.getDefault().getChart(chartName); chartPanel.repaint(); }
    public AbstractChart getChart() { return chart; }
    public void paintChart(Graphics2D g) { if (chart != null) chart.paint(g, this); }

    public String getTime() { return time; }
    public void setTime(String t) { time = t; chartRenderer.setMainDataset(stock, time); chartPanel.repaint(); }

    public ChartToolbar getChartToolbar() { return chartToolbar; }
    public ChartPanel getChartPanel() { return chartPanel; }
    public ChartProperties getChartProperties() { return chartProperties; }
    public ChartRenderer getChartRenderer() { return chartRenderer; }
    public JScrollBar getHorizontalScrollBar() { return horizontalBar; }
    public Marker getMarker() { return marker; }

    private JScrollBar initHorizontalScrollBar() {
        JScrollBar bar = new JScrollBar(JScrollBar.HORIZONTAL);
        BoundedRangeModel model = bar.getModel();
        model.setExtent(chartRenderer.getItems());
        model.setMinimum(0);
        model.setMaximum(chartRenderer.getEnd());
        model.setValue(chartRenderer.getEnd() - chartRenderer.getItems());
        bar.setModel(model);
        bar.setAlignmentX(java.awt.Component.RIGHT_ALIGNMENT);
        bar.addAdjustmentListener(this);
        return bar;
    }

    public void updateHorizontalScrollBar() {
        int value = chartRenderer.getEnd() - chartRenderer.getItems();

        BoundedRangeModel model = horizontalBar.getModel();
        model.setExtent(chartRenderer.getItems());
        model.setMinimum(0);
        model.setMaximum(chartRenderer.getMainDataset().getItemCount());
        model.setValue(value);
        horizontalBar.setModel(model);
    }

    public void updateHorizontalScrollBar(int end) {
        int i = end;
        i = i > chartRenderer.getMainDataset().getItemCount() ? chartRenderer.getMainDataset().getItemCount() : i;
        i = i < chartRenderer.getItems() ? chartRenderer.getItems() : i;
        int value = i - chartRenderer.getItems();

        BoundedRangeModel model = horizontalBar.getModel();
        model.setExtent(chartRenderer.getItems());
        model.setMinimum(0);
        model.setMaximum(chartRenderer.getMainDataset().getItemCount());
        model.setValue(value);
        horizontalBar.setModel(model);
    }

    public void adjustmentValueChanged(AdjustmentEvent e) {
        if (chartRenderer.getMainDataset() != null) {
            BoundedRangeModel model = horizontalBar.getModel();
            int end = model.getValue() + chartRenderer.getItems();
            end = end > chartRenderer.getMainDataset().getItemCount() ? chartRenderer.getMainDataset().getItemCount() : end;
            end = end < chartRenderer.getItems() ? chartRenderer.getItems() : end;
            chartRenderer.setEnd(end);
            chartPanel.repaint();
        }
    }

    public int getPersistenceType() { return PERSISTENCE_NEVER; }
    public String preferredID() { return id == null ? "ChartFrame" : id; }
    public void setID(String s) { id = s; }

    public void readXMLDocument(Element parent) {
        Element element;
        String t = XMLUtils.getStringParam(parent, "time");
        setTime(t);
        element = (Element) parent.getElementsByTagName("properties").item(0);
        getChartProperties().readXMLDocument(element);
        element = (Element) parent.getElementsByTagName("overlays").item(0);
        getChartRenderer().readOverlaysXMLDocument(element);
        element = (Element) parent.getElementsByTagName("indicators").item(0);
        getChartRenderer().readIndicatorsXMLDocument(element);
        element = (Element) parent.getElementsByTagName("annotations").item(0);
        getChartPanel().readAnnotationsXMLDocument(element);
        repaint();
    }

    public void writeXMLDocument(Document document, Element parent) {
        Element element;
        element = document.createElement("symbol");
        parent.appendChild(XMLUtils.setStringParam(element, getStock().getSymbol()));
        element = document.createElement("exchange");
        parent.appendChild(XMLUtils.setStringParam(element, getStock().getExchange()));
        element = document.createElement("time");
        parent.appendChild(XMLUtils.setStringParam(element, getTime()));
        element = document.createElement("chart");
        parent.appendChild(XMLUtils.setStringParam(element, getChart().getName()));
        element = document.createElement("frame");
        setPosition(document, element);
        parent.appendChild(element);
        element = document.createElement("properties");
        getChartProperties().writeXMLDocument(document, element);
        parent.appendChild(element);
        element = document.createElement("overlays");
        getChartRenderer().writeOverlaysXMLDocument(document, element);
        parent.appendChild(element);
        element = document.createElement("indicators");
        getChartRenderer().writeIndicatorsXMLDocument(document, element);
        parent.appendChild(element);
        element = document.createElement("annotations");
        getChartPanel().writeAnnotationsXMLDocument(document, element);
        parent.appendChild(element);
    }

    private void setPosition(Document document, Element parent) {
        Element element;
        element = document.createElement("tabPosition");
        parent.appendChild(XMLUtils.setIntegerParam(element, getTabPosition()));
        /*element = document.createElement("docked");
        parent.appendChild(XMLUtils.setBooleanParam(element, isDocked()));
        element = document.createElement("bounds");
        parent.appendChild(XMLUtils.setRectangleParam(element, org.openide.windows.WindowManager.getDefault().findMode(this).getBounds()));*/
    }

    /*private boolean isDocked() {
        java.awt.Frame frame = org.openide.windows.WindowManager.getDefault().getMainWindow();
        return (isOpened() && frame.equals(javax.swing.SwingUtilities.getWindowAncestor(this)));
    }*/

    class PeriodTimer extends TimerTask {
        public void run() {
            AbstractUpdater ac = UpdaterManager.getDefault().getActiveUpdater();
            if (ac != null) {
                int intraDay = (!time.contains("Min")) ? 0 : 1;
                Dataset dataset;
                switch (intraDay) {
                    case 0:
                        dataset = ac.updateLastValues(stock.getKey(), DatasetManager.DAILY, DatasetManager.getDefault().getDataset(DatasetManager.getName(stock, DatasetManager.DAILY)));
                        if (dataset != null) {
                            DatasetManager.getDefault().addDataset(DatasetManager.getName(stock, DatasetManager.DAILY), dataset);
                            if (time.equals(DatasetManager.DAILY)) {
                                getChartRenderer().setMainDataset(dataset, false);
                            }
                        }
                        break;
                    case 1:
                        dataset = ac.updateIntraDayLastValues(stock.getKey(), time, getChartRenderer().getMainDataset());
                        if (dataset != null) {
                            DatasetManager.getDefault().addDataset(DatasetManager.getName(stock, time), dataset);
                            getChartRenderer().setMainDataset(dataset, false);
                        }
                        break;
                }
                getChartPanel().repaint();
            }
        }
    }

}