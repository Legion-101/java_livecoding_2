import java.util.*;
import javax.swing.*;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) throws NumberFormatException, IOException {
        int MAX_FILES = 20;
        Set<String> files = new HashSet<String>();
        JFrame frame = new JFrame();
        String port = JOptionPane.showInputDialog(null, "Input port");
        frame.setTitle(port);
        synchronized (files) {
            for (String file : new File(port).list()) {
                files.add(file);
            }
        }
        System.out.println(files);
        ServerSocket serverSocket = new ServerSocket(Integer.parseInt(port));
        String ipWithPort = JOptionPane.showInputDialog(null, "Input ip:port " + port);

        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        Socket socket = serverSocket.accept();
                        new Thread(new Runnable() {
                            public void run() {
                                try {
                                    DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                                    DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                                    while (true) {
                                        String command = inputStream.readUTF();
                                        if (command.equals("ls")) {
                                            synchronized (files) {
                                                outputStream.writeInt(files.size());
                                                for (String file : files) {
                                                    outputStream.writeUTF(file);
                                                }
                                                outputStream.flush();
                                            }
                                        } else if (command.equals("download"))  {
                                            String nameFile = inputStream.readUTF();
                                            File file = new File(port + "/" +nameFile);
                                            outputStream.writeLong(file.length());
                                            FileInputStream fileStream = new FileInputStream(file);
                                            fileStream.transferTo(outputStream);
                                            fileStream.close();
                                            outputStream.flush();
                                        }
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        GridLayout layout = new GridLayout(MAX_FILES, 2);
        frame.setLayout(layout);
        JLabel[] labels = new JLabel[MAX_FILES];
        JButton[] buttons = new JButton[MAX_FILES];

        Socket socket = new Socket(ipWithPort.split(":")[0], Integer.parseInt(ipWithPort.split(":")[1]));
        DataInputStream inputStream = new DataInputStream(socket.getInputStream());
        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

        new Thread(new Runnable() {
            public void run() {
                try {
                    while (true) {
                        outputStream.writeUTF("ls");
                        outputStream.flush();
                        int size = inputStream.readInt();
                        for (int i = 0; i < size; i++) {
                            String nameFile = inputStream.readUTF();
                            labels[i].setText(nameFile);
                        }
                        frame.repaint();
                        Thread.sleep(100);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }).start();
        for (int i = 0; i < MAX_FILES; i++) {
            labels[i] = new JLabel();
            buttons[i] = new JButton();
            frame.add(labels[i]);
            frame.add(buttons[i]);
            int index = i;
            buttons[i].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    try {
                        String nameFile = labels[index].getText();
                        outputStream.writeUTF("download");
                        outputStream.writeUTF(nameFile);
                        outputStream.flush();
                        long size = inputStream.readLong();
                        byte[] buffer = new byte[4096];
                        FileOutputStream fileStream = new FileOutputStream(port + "/" + nameFile);
                        int offset = 0;
                        while (size > 0) {
                            int read = inputStream.read(buffer);
                            size -= read;
                            fileStream.write(buffer, offset, read);
                            offset += read;
                        }
                        fileStream.close();
                        synchronized (files) {
                            files.add(nameFile);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();

                    }
                }
            });
        }
        frame.setVisible(true);
        frame.setSize(480, 320);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}