package org.jivesoftware.sparkplugin;

import org.jivesoftware.spark.component.FileDragLabel;
import org.jivesoftware.spark.ui.ContactList;
import org.jivesoftware.spark.ui.ContactItem;
import org.jivesoftware.spark.SparkManager;
import org.jivesoftware.spark.util.SwingWorker;
import org.jivesoftware.spark.util.log.Log;
import org.jivesoftware.smackx.jingle.OutgoingJingleSession;
import org.jivesoftware.smackx.jingle.IncomingJingleSession;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.JingleSessionRequest;
import org.jivesoftware.resource.Res;
import org.jivesoftware.smack.XMPPException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CallMessage extends JPanel {

    private FileDragLabel imageLabel = new FileDragLabel();
    private JLabel titleLabel = new JLabel();
    private JLabel fileLabel = new JLabel();

    private CallMessage.CallButton cancelButton = new CallMessage.CallButton();
    private JingleSession session = null;
    private JingleSessionRequest request = null;

    private CallMessage.CallButton retryButton = new CallMessage.CallButton();
    private CallMessage.CallButton acceptButton = new CallMessage.CallButton();
    private String fullJID;

    private CallMessageCallback callback;

    public CallMessage(CallMessageCallback callback) {
        this.callback = callback;
        buildUI();
    }

    public CallMessage(CallMessageCallback callback, JingleSessionRequest request) {
        this.callback = callback;
        this.request = request;
        buildUI();
    }

    public void buildUI() {

        setLayout(new GridBagLayout());

        setBackground(new Color(250, 249, 242));
        add(imageLabel, new GridBagConstraints(0, 0, 1, 3, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

        add(titleLabel, new GridBagConstraints(1, 0, 2, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 11));
        titleLabel.setForeground(new Color(211, 174, 102));
        add(fileLabel, new GridBagConstraints(1, 1, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));

        cancelButton.setText(Res.getString("cancel"));
        retryButton.setText(Res.getString("retry"));
        acceptButton.setText("Accept");

        add(cancelButton, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
        add(retryButton, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
        add(acceptButton, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
        retryButton.setVisible(false);
        acceptButton.setVisible(false);

        retryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });

        final CallMessage callMessage = this;
        acceptButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                callback.acceptCall(callMessage);
            }
        });

        cancelButton.setForeground(new Color(73, 113, 196));
        cancelButton.setFont(new Font("Dialog", Font.BOLD, 11));
        cancelButton.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(73, 113, 196)));

        retryButton.setForeground(new Color(73, 113, 196));
        retryButton.setFont(new Font("Dialog", Font.BOLD, 11));
        retryButton.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(73, 113, 196)));

        acceptButton.setForeground(new Color(73, 113, 196));
        acceptButton.setFont(new Font("Dialog", Font.BOLD, 11));
        acceptButton.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(73, 113, 196)));

        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.white));

    }

    public void call(final JingleSession session, final String jid) {

        cancelButton.setVisible(true);
        retryButton.setVisible(false);
        this.fullJID = jid;

        this.session = session;

        fileLabel.setText(fullJID);

        ContactList contactList = SparkManager.getWorkspace().getContactList();
        ContactItem contactItem = contactList.getContactItemByJID(jid);

        titleLabel.setText("Calling: " + contactItem.getNickname());

        cancelButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent mouseEvent) {
                cancelCall();
            }

            public void mouseEntered(MouseEvent e) {
                cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            public void mouseExited(MouseEvent e) {
                cancelButton.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });

        SwingWorker worker = new SwingWorker() {
            public Object construct() {
                while (true) {
                    try {
                        Thread.sleep(100);
                        if (getSession() == null && getRequest() == null) {
                            break;
                        }
                    }
                    catch (InterruptedException e) {
                        Log.error("Unable to sleep thread.", e);
                    }
                    updateBar(getSession());
                }
                return "";
            }

            public void finished() {
                updateBar(getSession());
            }
        };

        worker.start();

        makeClickable(imageLabel);
        makeClickable(titleLabel);
    }

    private void updateBar(final JingleSession session) {
        if (session == null && request == null) {
            titleLabel.setText("Call Ended");
            cancelButton.setVisible(true);
            retryButton.setVisible(false);
            acceptButton.setVisible(false);
        } else if (session == null) {
            showAlert(true);
            titleLabel.setText("Incoming Call");
            cancelButton.setVisible(false);
            retryButton.setVisible(false);
            acceptButton.setVisible(true);
        } else if (session instanceof OutgoingJingleSession) {
            acceptButton.setVisible(false);
            showAlert(false);
            if (session.getState() instanceof OutgoingJingleSession.Active) {
                cancelButton.setVisible(true);
                retryButton.setVisible(false);
                titleLabel.setText("On Phone");
            } else if (session.getState() instanceof OutgoingJingleSession.Inviting) {
                cancelButton.setVisible(true);
                retryButton.setVisible(false);
                titleLabel.setText("Calling...");
            } else if (session.getState() instanceof OutgoingJingleSession.Pending) {
                titleLabel.setText("Ringing");
                cancelButton.setVisible(true);
                retryButton.setVisible(false);
            }
        } else if (session instanceof IncomingJingleSession) {
            acceptButton.setVisible(false);
            showAlert(false);
            if (session.getState() instanceof IncomingJingleSession.Accepting) {
                titleLabel.setText("Accepting...");
                cancelButton.setVisible(true);
            } else if (session.getState() instanceof IncomingJingleSession.Pending) {
                titleLabel.setText("Establishing...");
                cancelButton.setVisible(true);
            } else if (session.getState() instanceof IncomingJingleSession.Active) {
                titleLabel.setText("On Phone");
                cancelButton.setVisible(true);
            }
        }
    }

    private void makeClickable(final JLabel label) {
        label.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
                label.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            public void mouseExited(MouseEvent e) {
                label.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
    }

    private class CallButton extends JButton {

        public CallButton() {
            decorate();
        }

        /**
         * Create a new RolloverButton.
         *
         * @param text the button text.
         * @param icon the button icon.
         */
        public CallButton(String text, Icon icon) {
            super(text, icon);
            decorate();
        }

        /**
         * Decorates the button with the approriate UI configurations.
         */
        private void decorate() {
            setBorderPainted(false);
            setOpaque(true);

            setContentAreaFilled(false);
            setMargin(new Insets(1, 1, 1, 1));
        }

    }

    private void showAlert(boolean alert) {
        if (alert) {
            titleLabel.setForeground(new Color(211, 174, 102));
            setBackground(new Color(250, 249, 242));
        } else {
            setBackground(new Color(239, 245, 250));
            titleLabel.setForeground(new Color(65, 139, 179));
        }
    }

    public void cancelCall() {
        if (session != null) {
            try {
                session.terminate();
                session = null;
                request = null;
            } catch (XMPPException e) {
                e.printStackTrace();
            }
        }else if(request!=null){
            request.reject();
        }
    }

    public JingleSession getSession() {
        return session;
    }

    public void setSession(JingleSession session) {
        this.session = session;
    }

    public JingleSessionRequest getRequest() {
        return request;
    }

    public void setRequest(JingleSessionRequest request) {
        this.request = request;
    }
}
