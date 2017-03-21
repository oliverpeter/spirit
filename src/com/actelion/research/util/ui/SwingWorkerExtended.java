/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2016 Actelion Pharmaceuticals Ltd., Gewerbestrasse 16,
 * CH-4123 Allschwil, Switzerland.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 * @author Joel Freyss
 */

package com.actelion.research.util.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;


/**
 * Simple Wrapper to a SwingWorker. It is used to execute longer processes on the background, while showing that something is going on.
 * This class uses the SwingWorkerExecutor to maximize the number of possible threads (2 + the main thread)
 *
 * @author jfreyss
 *
 */
public class SwingWorkerExtended  {

	public static boolean DEBUG = false;

	private static final ExecutorService bgPool = Executors.newWorkStealingPool();


	public static final int FLAG_SYNCHRONOUS = 0;
	public static final int FLAG_ASYNCHRONOUS = 2;
	public static final int FLAG_ASYNCHRONOUS20MS = 20;
	public static final int FLAG_ASYNCHRONOUS100MS = 100;
	public static final int FLAG_ASYNCHRONOUS500MS = 500;

	private final String name;
	private JFrame frame = null;
	private JDialog dialog = null;

	private Thread sw;
	private Thread delayVisibleThread;
	private static int instances;
	private final AtomicBoolean bgTaskStarted = new AtomicBoolean(false);
	private final AtomicBoolean longTaskDone = new AtomicBoolean(false);
	private final static Map<Component, Map<String, Thread>> currentThreads = new HashMap<>();
	private Runnable after;

	public SwingWorkerExtended() {
		this(null, null, FLAG_ASYNCHRONOUS20MS);
	}

	public SwingWorkerExtended(final String title, Component myComp) {
		this(title, myComp, FLAG_ASYNCHRONOUS20MS);
	}

	public SwingWorkerExtended(final Component myComp, final int flags) {
		this(null, myComp, flags);
	}

	/**
	 * Creates a SwingWorker
	 * @param comp
	 * @param cancellable
	 */
	public SwingWorkerExtended(final String title, final Component myComp, final int delayInMs) {
		final long started = System.currentTimeMillis();
		this.name = (instances++) + "-" + (title==null?"SwingWorker":title);
		final Component comp = (myComp!=null && myComp.getWidth()==0) && UIUtils.getMainFrame()!=null? ((JFrame)UIUtils.getMainFrame()).getContentPane():
			(myComp instanceof JFrame)? ((JFrame)myComp).getContentPane():
				(myComp instanceof JDialog)? ((JDialog)myComp).getContentPane():
					myComp;

				String last = "";
				if(DEBUG) {
					StackTraceElement[] t = Thread.currentThread().getStackTrace();
					for (StackTraceElement stackTraceElement : t) {
						if(stackTraceElement.getClassName().startsWith("com.actelion") && !stackTraceElement.getClassName().startsWith("com.actelion.research.gui.util")) {
							last = stackTraceElement.toString();
							break;
						}
					}
					System.out.println("SwingWorkerExtended " + name + " -START- " + last);
				}

				final String callingThread = Thread.currentThread().getName();
				final Runnable doneRunnable = new Runnable() {
					@Override
					public void run() {
						SwingWorkerExtended.this.done();
						if(after!=null) after.run();
					}
				};

				if(delayInMs==0) {
					//SYNCHRONOUS MODE: call doBackground in the same thread
					try {
						if(DEBUG) System.out.println("SwingWorkerExtended " + name + " -BG- " + (System.currentTimeMillis()-started) + "ms - " + callingThread);
						doInBackground();

						SwingUtilities.invokeLater(doneRunnable);

					} catch(Exception e) {
						JExceptionDialog.showError(comp, e);
					} finally {
						endBgProcess();
					}
					if(DEBUG) System.out.println("SwingWorkerExtended "+name+" -DONE- " + (System.currentTimeMillis()-started) + "ms - " +callingThread);
				} else {
					//ASYNCHRONOUS MODE: call doBackground in a separate thread
					sw = new Thread(name) {
						@Override
						public void run() {

							if(isCancelled()) {
								endBgProcess();
								if(DEBUG) System.out.println("SwingWorkerExtended "+name+" -STOP- " + (System.currentTimeMillis()-started) + "ms - " + callingThread);
								return;
							}

							if(comp!=null && delayInMs>0) {
								try {
									sleep(delayInMs);
								} catch(Throwable e) {
									endBgProcess();
									if(DEBUG) System.out.println("SwingWorkerExtended "+name+" -STOP- " +callingThread);
									return;
								}
							}
							bgPool.submit(new Runnable() {
								@Override
								public void run() {
									try {
										//In Background
										if(DEBUG) System.out.println("SwingWorkerExtended " + name + " -BG- " + (System.currentTimeMillis()-started) + "ms - " + callingThread);
										SwingWorkerExtended.this.doInBackground();
									} catch (final Throwable thrown) {
										if(!isCancelled() && !isInterrupted()) {
											JExceptionDialog.showError(comp, thrown);
											if(DEBUG) System.out.println("SwingWorkerExtended " + name + " -DONE- " + (System.currentTimeMillis()-started) + "ms - " + callingThread);
										}
										return;
									} finally {
										endBgProcess();
									}

									if(isCancelled()) {
										endBgProcess();
										if(DEBUG) System.out.println("SwingWorkerExtended " + name + " -CANCEL- " + (System.currentTimeMillis()-started) + "ms - " + callingThread);
										return;
									}

									SwingUtilities.invokeLater(doneRunnable);
									if(DEBUG) System.out.println("SwingWorkerExtended "+name+" -DONE- " + (System.currentTimeMillis()-started) + "ms - " +callingThread);
								}
							});
						}

					};
					sw.setDaemon(!callingThread.contains("main"));
					sw.start();

					//Interrupt threads with the same name if start was delayed
					if(comp!=null && delayInMs>0) {
						if(currentThreads.get(comp)==null) currentThreads.put(comp, new HashMap<String, Thread>());
						Thread t2 = currentThreads.get(comp).get(title);
						if(t2!=null && t2.isAlive()) {
							System.out.println("SwingWorkerExtended() "+name+" Interrupt "+t2+" "+t2.isInterrupted());
							t2.interrupt();
							try{t2.join(10000);}catch (Exception e) {e.printStackTrace();}
						}
						currentThreads.get(comp).put(title, sw);
					}

					//Show the loading panel if the task takes more than 300m
					delayVisibleThread = new Thread("SwingWorkerExtended-GlassPane") {
						@Override
						public void run() {
							try{Thread.sleep(Math.max(delayInMs, 200));} catch(Exception e) {return;}
							System.out.println("SwingWorkerExtended.startBgProcess "+comp);
							startBgProcess(title, comp);
						}
					};
					delayVisibleThread.setDaemon(true);
					delayVisibleThread.start();
				}
	}


	public String getName() {
		return name;
	}


	/**
	 *
	 * @param comp
	 * @param cancellable
	 */
	private void startBgProcess(final String title, final Component comp) {
		if(comp==null || !comp.isShowing() || longTaskDone.get()) {
			return;
		}

		if(!(comp instanceof JComponent)) {
			System.err.println("SwingWorkerExtended: component not a JComponent: " + comp.getClass());
			return;
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if(!comp.isShowing() || longTaskDone.get()) {
					return;
				}
				bgTaskStarted.set(true);

				if(((JComponent)comp).getTopLevelAncestor() instanceof JFrame) {
					frame = (JFrame) ((JComponent)comp).getTopLevelAncestor();
					if(frame.getGlassPane().isVisible()) return;
					if(frame.isUndecorated()) return;
				} else if(((JComponent)comp).getTopLevelAncestor() instanceof JDialog) {
					dialog = (JDialog) ((JComponent)comp).getTopLevelAncestor();
					if(dialog.getGlassPane().isVisible()) return;
				} else {
					System.err.println("SwingWorkerExtended: getTopLevelAncestor not a jframe or jdialog -> "+ ((JComponent)comp).getTopLevelAncestor());
					return;
				}

				Component onComponent =  comp;
				while(onComponent.getParent()!=null && (onComponent.getHeight()==0 || (onComponent.getParent() instanceof JViewport) )) {
					onComponent = comp.getParent();
				}

				final Point p1 = frame!=null? frame.getRootPane().getLocationOnScreen():  dialog.getRootPane().getLocationOnScreen();
				final Point p2 = onComponent.getLocationOnScreen();
				final Rectangle r = new Rectangle(p2.x-p1.x, p2.y-p1.y, onComponent.getWidth(), onComponent.getHeight());
				if(r.width==0 || r.height==0) {
					System.err.println("SwingWorkerExtended: component not yet realized");
					return;
				}

				System.out.println("SwingWorkerExtended.startBgProcess(...) "+r);
				final JComponent glassPane = new JPanel() {
					@Override
					public void paintComponent(Graphics g) {
						if (g instanceof Graphics2D) {
							Color c1 = new Color(220,120,120,0);
							Color c2 = new Color(255,255,255,255);

							Graphics2D g2d = (Graphics2D) g;
							g2d.setPaint(new GradientPaint(r.x, r.y, c1, r.x + r.width/2, r.y + r.height/2, c2, true));
							g2d.fillRect(r.x, r.y, r.width/2, r.height/2);

							g2d.setPaint(new GradientPaint(r.x + r.width, r.y + r.height, c1, r.x + r.width/2, r.y + r.height/2, c2, true));
							g2d.fillRect(r.x + r.width/2, r.y + r.height/2, r.width/2, r.height/2);

							g2d.setPaint(new GradientPaint(r.x + r.width, r.y, c1, r.x + r.width/2, r.y + r.height/2, c2, true));
							g2d.fillRect(r.x + r.width/2, r.y, r.width/2, r.height/2);

							g2d.setPaint(new GradientPaint(r.x, r.y + r.height, c1, r.x + r.width/2, r.y + r.height/2, c2, true));
							g2d.fillRect(r.x, r.y + r.height/2, r.width/2, r.height/2);
						}

						super.paintComponent(g);
					}
				};

				glassPane.setOpaque(false);
				glassPane.setLayout(null);

				JLabel bar;
				try {
					URL url = getClass().getResource("progress.gif");
					ImageIcon icon = new ImageIcon(url);
					bar = new JLabel(icon);
				} catch (Exception e) {
					e.printStackTrace();
					bar = new JLabel("");
				}
				bar.setText(title);
				bar.setVerticalTextPosition(SwingConstants.BOTTOM);
				bar.setHorizontalTextPosition(SwingConstants.CENTER);
				Dimension dim = bar.getPreferredSize();
				bar.setBounds(r.x + r.width/2-dim.width/2, r.y + r.height/2-dim.height/2, dim.width, dim.height);
				glassPane.add(bar);


				{
					JButton stopButton = new JButton("X");
					stopButton.setToolTipText("Stop the task");
					stopButton.setBorderPainted(false);
					stopButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
					Dimension d = new Dimension(18,18);
					stopButton.setBounds(r.x + r.width/2 + dim.width/2 - d.width, r.y + r.height/2-dim.height/2, d.width, d.height);
					glassPane.add(stopButton);
					ActionListener cancelAction = e-> {
						endBgProcess();
						sw.interrupt();
					};
					stopButton.addActionListener(cancelAction);
					KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
					glassPane.registerKeyboardAction(cancelAction, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
					stopButton.addActionListener(cancelAction);

				}


				if(frame!=null) {
					frame.setGlassPane(glassPane);
				} else {
					dialog.setGlassPane(glassPane);
				}
				glassPane.setVisible(true);
				glassPane.repaint();
			}
		});

	}

	private void endBgProcess() {
		longTaskDone.set(true);
		if(bgTaskStarted.get()) {
			try {
				delayVisibleThread.interrupt();
				try {delayVisibleThread.join(1000);}catch(Exception e) {return;}
			} finally {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						if(frame!=null) {
							frame.getGlassPane().setVisible(false);
						} else if(dialog!=null) {
							dialog.getGlassPane().setVisible(false);
						}
					}
				});
			}
		}
	}

	public void cancel() {
		sw.interrupt();
	}

	protected boolean isCancelled() {
		return sw.isInterrupted();
	}

	protected void doInBackground() throws Exception {}
	protected void done() {}

	/**
	 * Runnable function to be executed  in the same thread after successful completion
	 * @param after
	 */
	public void afterDone(Runnable after) {
		this.after = after;
	}

	@Override
	public String toString() {
		return "[SW:"+name+"]";
	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(new NimbusLookAndFeel());
		} catch (Exception e) {
			e.printStackTrace();
		}

		JLabel label = new JLabel("Test in bg");

		JFrame frame = new JFrame("Test");
		frame.setContentPane(UIUtils.createCenterPanel(label, false));
		frame.setSize(500, 500);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

		SwingWorkerExtended.DEBUG = true;
		new SwingWorkerExtended("Long long Long task", frame.getContentPane(), SwingWorkerExtended.FLAG_ASYNCHRONOUS) {
			@Override
			protected void doInBackground() throws Exception {
				Thread.sleep(5000);
			}
			@Override
			protected void done() {
				label.setText("Done");
				System.exit(1);
			}
		};

	}

}
