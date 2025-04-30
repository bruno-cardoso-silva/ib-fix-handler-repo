package org.ib.fix.model;

public class RawFixMessage {
   private final String fixMessage;

   public RawFixMessage(String fixMessage) {
       this.fixMessage = fixMessage;
   }
   public String getFixMessage() {
       return this.fixMessage.replace("^", "\u0001");
   }

    @Override
    public String toString() {
        return fixMessage;
    }

}
