
package edu.buffalo.cse.cse486586.groupmessenger;

import java.io.Serializable;

public class MessageWrapper implements Serializable {

    private static final long serialVersionUID = 1L;
    private String msg;
    private String identifier;
    private boolean multicast = true;
    private int seq_no;

    /***
     * Get the message
     * 
     * @return Message as String
     */
    public String getMsg() {
        return msg;
    }

    /***
     * Set the Message
     * 
     * @param msg
     */
    public void setMsg(String msg) {
        this.msg = msg;
    }

    /***
     * Get Identifier as avd0/avd1/avd2/avd3/avd4
     * 
     * @return AVD number as String
     */
    public String getIdentifier() {
        return identifier;
    }

    /***
     * Set the Identifier by using the REMOTE_PORT number
     * 
     * @param identifier
     */
    public void setIdentifier(String identifier) {
        if (identifier.equals(GroupMessengerActivity.REMOTE_PORT0)) {
            this.identifier = "avd0";
        } else if (identifier.equals(GroupMessengerActivity.REMOTE_PORT1)) {
            this.identifier = "avd1";
        } else if (identifier.equals(GroupMessengerActivity.REMOTE_PORT2)) {
            this.identifier = "avd2";
        } else if (identifier.equals(GroupMessengerActivity.REMOTE_PORT3)) {
            this.identifier = "avd3";
        } else if (identifier.equals(GroupMessengerActivity.REMOTE_PORT4)) {
            this.identifier = "avd4";
        }
    }

    /***
     * Check if the message has to be multicasted or has been multicasted.
     * 
     * @return true if it has to be multicasted, false if it has been
     *         multicasted.
     */
    public boolean isMulticast() {
        return multicast;
    }

    /***
     * Set if a message as multicasted or not multicasted.
     * 
     * @param multicast
     */
    public void setMulticast(boolean multicast) {
        this.multicast = multicast;
    }

    /***
     * Get The Seuence number for this message.
     * 
     * @return Sequence number as Integer
     */
    public int getSeq_no() {
        return seq_no;
    }

    /***
     * Set the sequence number to an Integer value.
     * 
     * @param seq_no
     */
    public void setSeq_no(int seq_no) {
        this.seq_no = seq_no;
    }
}
