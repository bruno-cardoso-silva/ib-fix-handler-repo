package org.ib.fix.model;

public class RawFixMessage {
   private String fixMessage;

   public RawFixMessage(String fixMessage) {
       this.fixMessage = fixMessage;
   }
   public String getFixMessage() {
       return this.fixMessage.replace("^", "\u0001");
   }
}
