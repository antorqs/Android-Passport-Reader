package passportreader.retrovanilla.com.androidpassportreader.jmrtd;

import net.sourceforge.scuba.smartcards.CardService;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.smartcards.CommandAPDU;
import net.sourceforge.scuba.smartcards.ResponseAPDU;
import net.sourceforge.scuba.util.Hex;

import org.jmrtd.BACEvent;
import org.jmrtd.JMRTDSecurityProvider;
import org.jmrtd.PassportService;
import org.jmrtd.SecureMessagingWrapper;
import org.jmrtd.Util;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import passportreader.retrovanilla.com.androidpassportreader.data.Document;

/**
 * Created by antonio on 12/01/16.
 */
public class OwnPassportService extends PassportService {
    private static final IvParameterSpec ZERO_IV_PARAM_SPEC = new IvParameterSpec(new byte[8]);
    private transient Cipher cipher;
    private transient Mac mac;
    private boolean replay = false;
    private byte[] oldMac;
    private byte[] oldMessage;
    private Document doc;
    private StringBuilder output;
    private long tStart, tEnd, timing;


    public OwnPassportService(CardService service) throws CardServiceException {
        this(service,false, null, null);
    }

    public OwnPassportService(CardService service, boolean replay, byte[] oldMac, byte[] oldMessage)
            throws CardServiceException {
        super(service);
        this.replay = replay;
        this.oldMac = oldMac;
        this.oldMessage = oldMessage;
        this.output = new StringBuilder();
        try {
            this.mac = Mac.getInstance("ISO9797Alg3Mac", JMRTDSecurityProvider.getBouncyCastleProvider());
            this.cipher = Cipher.getInstance("DESede/CBC/NoPadding");
        } catch (GeneralSecurityException var3) {
            var3.printStackTrace();
            throw new CardServiceException(var3.toString());
        }
    }

    @Override
    public synchronized void doBAC(SecretKey kEnc, SecretKey kMac)
            throws CardServiceException, GeneralSecurityException {
        output.append("<br />[BAC] Starting Communication...<br />");
        output.append("[BAC] Sending challenge...<br />");
        byte[] rndICC = this.sendGetChallenge();
        byte[] p_rndICC =  new byte[9];
        p_rndICC[0] = 0;
        System.arraycopy(rndICC, 0, p_rndICC, 1, 8);
        output.append("[BAC] Got Response [Nt = ").append(new BigInteger(p_rndICC).toString()).append("]<br />");
        byte[] rndIFD = new byte[8];
        this.random.nextBytes(rndIFD);
        byte[] kIFD = new byte[16];
        this.random.nextBytes(kIFD);
        output.append("[BAC] Sending Mutual Auth...<br />");
        byte[] response = this.sendMutualAuth(rndIFD, rndICC, kIFD, kEnc, kMac);
        byte[] kICC = new byte[16];
        System.arraycopy(response, 16, kICC, 0, 16);
        byte[] keySeed = new byte[16];

        for(int ksEnc = 0; ksEnc < 16; ++ksEnc) {
            keySeed[ksEnc] = (byte)(kIFD[ksEnc] & 255 ^ kICC[ksEnc] & 255);
        }

        SecretKey var14 = Util.deriveKey(keySeed, 1);
        SecretKey ksMac = Util.deriveKey(keySeed, 2);
        long ssc = Util.computeSendSequenceCounter(rndICC, rndIFD);
        this.wrapper = new SecureMessagingWrapper(var14, ksMac, ssc);
        BACEvent event = new BACEvent(this, rndICC, rndIFD, kICC, kIFD, true);
        this.notifyBACPerformed(event);
        //this.state = 2;
    }

    @Override
    public synchronized byte[] sendMutualAuth(byte[] rndIFD, byte[] rndICC,
                                              byte[] kIFD, SecretKey kEnc,
                                              SecretKey kMac) throws CardServiceException {
        String outMsg = "";
        String dataString = "";
        try {
            if(rndIFD != null && rndIFD.length == 8) {
                if(rndICC == null || rndICC.length != 8) {
                    rndICC = new byte[8];
                }

                if(kIFD != null && kIFD.length == 16) {
                    if(kEnc == null) {
                        throw new IllegalArgumentException("kEnc == null");
                    } else if(kMac == null) {
                        throw new IllegalArgumentException("kMac == null");
                    } else {
                        this.cipher.init(1, kEnc, ZERO_IV_PARAM_SPEC);
                        byte[] gse = new byte[32];
                        System.arraycopy(rndIFD, 0, gse, 0, 8);
                        System.arraycopy(rndICC, 0, gse, 8, 8);
                        System.arraycopy(kIFD, 0, gse, 16, 16);
                        byte[] ciphertext = this.cipher.doFinal(gse);
                        dataString = Hex.bytesToHexString(ciphertext);

                        String color = "<font color='#357EC7'>";
                        String color2 = "<font color='#4E387E'>";

                        outMsg = "[Encrypted data]: " + dataString + "[END]";
                        output.append("[MAU Encrypted Data]<br />");
                        output.append("<br />\t\t"+color+"<b>").append(dataString.substring(0, 16)).append("</b><br />");
                        output.append("\t\t<b>").append(dataString.substring(16, 32)).append("</b><br />");
                        output.append("\t\t<b>").append(dataString.substring(32, 48)).append("</b><br />");
                        output.append("\t\t<b>").append(dataString.substring(48, 64)).append("</b></font><br /><br />");
                        System.err.println(outMsg);
                        if(replay) {
                            color = "<font color='#CC6600'>";
                            dataString = Hex.bytesToHexString(oldMessage);
                            outMsg = "[OLD Encrypted ]: " + dataString + "[END]";
                            output.append("[MAU Old Enc Data]<br />");
                            output.append("<br />\t\t"+color+"<b>").append(dataString.substring(0, 16)).append("</b><br />");
                            output.append("\t\t<b>").append(dataString.substring(16, 32)).append("</b><br />");
                            output.append("\t\t<b>").append(dataString.substring(32, 48)).append("</b><br />");
                            output.append("\t\t<b>").append(dataString.substring(48, 64)).append("</b></font><br /><br />");
                            System.err.println(outMsg);
                        }
                        System.err.flush();
                        if(ciphertext.length != 32) {
                            outMsg = "Cryptogram wrong length " + ciphertext.length;
                            output.append("[MAU] ERR: ").append(outMsg).append("<br />");
                            throw new IllegalStateException(outMsg);
                        } else {
                            this.mac.init(kMac);
                            byte[] mactext = this.mac.doFinal(Util.pad(ciphertext));

                            dataString = Hex.bytesToHexString(mactext);
                            outMsg = "[MAC COMPUTED]: " + dataString + "[END]";
                            output.append("[MAU MAC COMPUTED] "+color2+"<b>").append(dataString).append("</b></font><br /><br />");
                            System.err.println(outMsg);
                            if(replay) {
                                color2 = "<font color='#7F462C'>";
                                dataString = Hex.bytesToHexString(oldMac);
                                outMsg = "[OLD MAC COMP]: " + dataString + "[END]";
                                output.append("[MAU OLD MAC COMP] "+color2+"<b>").append(dataString).append("</b></font><br /><br />");
                                System.err.println(outMsg);
                            }else{
                                updateDocument(mactext, ciphertext);
                            }
                            System.err.flush();
                            if(mactext.length != 8) {
                                outMsg = "MAC wrong length";
                                output.append("[MAU] ERR:").append(outMsg).append("<br />");
                                throw new IllegalStateException(outMsg);
                            } else {
                                byte p1 = 0;
                                byte p2 = 0;
                                byte[] data = new byte[40];
                                if(replay){
                                    System.arraycopy(oldMessage, 0, data, 0, 32);
                                    System.arraycopy(oldMac, 0, data, 32, 8);
                                }else {
                                    System.arraycopy(ciphertext, 0, data, 0, 32);
                                    System.arraycopy(mactext, 0, data, 32, 8);
                                }
                                byte le = 40;
                                dataString = Hex.bytesToHexString(data);
                                output.append("[MAU FULL MESSAGE]<br />");
                                output.append("<br />\t\t"+color+"<b>").append(dataString.substring(0, 16)).append("</b><br />");
                                output.append("\t\t<b>").append(dataString.substring(16, 32)).append("</b><br />");
                                output.append("\t\t<b>").append(dataString.substring(32, 48)).append("</b><br />");
                                output.append("\t\t<b>").append(dataString.substring(48, 64)).append("</b></font><br />");
                                output.append("\t\t"+color2+"<b>").append(dataString.substring(64, 80)).append("</b></font><br /><br />");
                                outMsg = "[FULL MESSAGE]: " + dataString + "[END]";
                                System.err.println(outMsg);
                                System.err.flush();

                                CommandAPDU capdu = new CommandAPDU(0, -126, p1, p2, data, le);
                                tStart = System.currentTimeMillis();
                                ResponseAPDU rapdu = this.transmit(capdu);
                                tEnd = System.currentTimeMillis();
                                timing = tEnd-tStart;
                                outMsg = rapdu.toString();
                                output.append("[MAU] ").append("<font color='#348017'><b>Communication Time: ").append(timing).append(" ms.</b></font><br />");
                                System.err.println(outMsg);
                                System.err.flush();
                                byte[] rapduBytes = rapdu.getBytes();
                                short sw = (short)rapdu.getSW();

                                if(sw == -28672){
                                    output.append("[MAU] <font color='#348017'>");
                                }else{
                                    output.append("[MAU] <font color='#E41B17'>");
                                }
                                output.append(outMsg).append("</font><br />");

                                if(rapduBytes == null) {
                                    outMsg = "Mutual authentication failed";
                                    output.append("[MAU] ").append(outMsg).append(" SW: ").append(sw).append("<br />");
                                    throw new CardServiceException(outMsg, sw);
                                } else {
                                    if(sw != -28672) {  //if no success
                                        le = 0;
                                        capdu = new CommandAPDU(0, -126, p1, p2, data, le);
                                        rapdu = this.transmit(capdu);
                                        rapduBytes = rapdu.getBytes();
                                        sw = (short)rapdu.getSW();
                                        outMsg = rapdu.toString();
                                        output.append("[MAU] <font color='#E41B17'>").append(outMsg).append("</font><br />");
                                        System.err.println(outMsg);
                                        System.err.flush();
                                    }

                                    if(rapduBytes.length != 42) {
                                        outMsg = "Mutual Auth failed: expected length: 40 + 2, length: " + rapduBytes.length;
                                        output.append("[MAU] ").append(outMsg).append("<br />").append(rapdu.toString()).append("<br />");
                                        throw new CardServiceException(outMsg, sw);
                                    } else {
                                        this.cipher.init(2, kEnc, ZERO_IV_PARAM_SPEC);
                                        byte[] result = this.cipher.doFinal(rapduBytes, 0, rapduBytes.length - 8 - 2);
                                        if(result.length != 32) {
                                            outMsg = "Cryptogram wrong length " + result.length;
                                            output.append("[MAU] ").append(outMsg).append(" SW: ").append(sw).append("<br />");
                                            throw new IllegalStateException(outMsg);
                                        } else {
                                            return result;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    outMsg = "kIFD wrong length";
                    output.append("[MAU] ").append(outMsg).append("<br />");
                    throw new IllegalArgumentException(outMsg);
                }
            } else {
                outMsg = "rndIFD wrong length";
                output.append("[MAU] ").append(outMsg).append("<br />");
                throw new IllegalArgumentException(outMsg);
            }
        } catch (GeneralSecurityException var18) {
            throw new CardServiceException(var18.toString());
        }
    }

    public void setDoc(Document doc) {
        this.doc = doc;
    }

    private void updateDocument(byte[] mac, byte[] message){
        doc.setMac(mac);
        doc.setMessage(message);
        doc.update();
    }

    public StringBuilder getOutput() {
        return output;
    }

    public long getTotalTime(){
        return timing;
    }

    public static long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE);
        buffer.put(bytes);
        buffer.flip();//need flip
        return buffer.getLong();
    }

}
