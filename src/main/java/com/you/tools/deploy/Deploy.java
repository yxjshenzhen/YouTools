package com.you.tools.deploy;

import com.jcraft.jsch.*;
import org.apache.commons.cli.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;

/**
 * 上传文件到目标服务器并执行shell命令
 */
public class Deploy {
    private static Options OPTIONS = new Options();
    private static CommandLine commandLine;
    private static String HELP_STRING = null;
    private static ScpInfo scpInfo = new ScpInfo();
    private static ExecInfo execInfo = new ExecInfo();
    private static ConnectInfo connectInfo = new ConnectInfo();
    private static Session session = null;

    public static void main(String[] args) {
        initCliArgs(args);
        openSession();
        scp();
        exec();
        closeSession();
        exit();
    }

    private static void openSession() {
        String host = connectInfo.getHost();
        String user = connectInfo.getUser();
        String password = connectInfo.getPassword();
        int port = connectInfo.getPort();

        JSch jsch = new JSch();
        try {
            session = jsch.getSession(user, host, port);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setPassword(password);
            session.connect();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private static void closeSession() {
        if (session != null) {
            session.disconnect();
        }
    }

    private static void exit() {
        System.exit(0);
    }

    private static void scp() {
        System.out.println("开始上传文件包,请稍等...");
        try {
            String srcPath = scpInfo.getSrcPath();
            String dstPath = scpInfo.getDstPath();
            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp c = (ChannelSftp) channel;

            c.put(srcPath, dstPath, ChannelSftp.OVERWRITE);
            System.out.println("文件包已上传成功！");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private static void exec() {
        System.out.println("开始执行远程命令...");
        try {
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(execInfo.getCommand());

            channel.setInputStream(null);
            channel.setOutputStream(System.out);
            ((ChannelExec) channel).setErrStream(System.err);

            InputStream in = channel.getInputStream();

            channel.connect();

            System.out.println("正在执行命令：  " + execInfo.getCommand());

            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    System.out.print(new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    if (in.available() > 0) continue;
                    if(channel.getExitStatus() != 0){
                        System.out.println("exit: " + channel.getExitStatus());
                    }
                    break;
                }
            }

            System.out.println("部署完毕！");
            System.out.println("请前往对应服务器查看启动日志！！！");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
     * init args
     *
     * @param args args
     */
    private static void initCliArgs(String[] args) {
        // validate args
        {
            CommandLineParser commandLineParser = new DefaultParser();
            // help
            OPTIONS.addOption("help","usage help");
            // host
            OPTIONS.addOption(Option.builder("h").required().hasArg(true).longOpt("host").type(String.class).desc("the host of remote server").build());
            // port
            OPTIONS.addOption(Option.builder("P").hasArg(true).longOpt("port").type(Short.TYPE).desc("the port of remote server").build());
            // user
            OPTIONS.addOption(Option.builder("u").required().hasArg(true).longOpt("user").type(String.class).desc("the user of remote server").build());
            // password
            OPTIONS.addOption(Option.builder("p").required().hasArg(true).longOpt("password").type(String.class).desc("the password of remote server").build());
            // srcPath
            OPTIONS.addOption(Option.builder("s").required().hasArg(true).longOpt("src_path").type(String.class).desc("the srcPath of local").build());
            // dstPath
            OPTIONS.addOption(Option.builder("d").required().hasArg(true).longOpt("dst_path").type(String.class).desc("the dstPath of remote").build());
            // shell
            OPTIONS.addOption(Option.builder("c").required().hasArg(true).longOpt("command").type(String.class).desc("will exec command").build());
            try {
                commandLine = commandLineParser.parse(OPTIONS, args);
            } catch (ParseException e) {
                System.out.println(e.getMessage() + "\n" + getHelpString());
                System.exit(0);
            }
        }

        // init serverConfigure
        {
            if(commandLine.hasOption("help")){
                System.out.println("\n" + getHelpString());
                System.exit(1);
            }

            // host
            connectInfo.setHost(commandLine.getOptionValue("h"));
            // port
            String portOptionValue = commandLine.getOptionValue("P");
            short port = portOptionValue == null || "".equals(portOptionValue) ? 22 : Short.parseShort(portOptionValue);
            connectInfo.setPort(port);
            // user
            connectInfo.setUser(commandLine.getOptionValue("u"));
            // password
            connectInfo.setPassword(commandLine.getOptionValue("p"));
            // srcPath
            scpInfo.setSrcPath(commandLine.getOptionValue("s"));
            // dstPath
            scpInfo.setDstPath(commandLine.getOptionValue("d"));
            // shell
            execInfo.setCommand(commandLine.getOptionValue("c"));
        }

    }

    /**
     * get string of help usage
     *
     * @return help string
     */
    private static String getHelpString() {
        if (HELP_STRING == null) {
            HelpFormatter helpFormatter = new HelpFormatter();

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            PrintWriter printWriter = new PrintWriter(byteArrayOutputStream);
            helpFormatter.printHelp(printWriter, HelpFormatter.DEFAULT_WIDTH, "deploy -help", null,
                    OPTIONS, HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, null);
            printWriter.flush();
            HELP_STRING = new String(byteArrayOutputStream.toByteArray());
            printWriter.close();
        }
        return HELP_STRING;
    }

}
