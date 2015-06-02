import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;


/** Simple example of JNA interface mapping and usage. */
public class EEGServ {

	public static final int PORT = 50007;

    public static void main(String[] args)
    {
    	Pointer eEvent			= Edk.INSTANCE.EE_EmoEngineEventCreate();
    	Pointer eState			= Edk.INSTANCE.EE_EmoStateCreate();
    	IntByReference userID 	= null;
    	short composerPort		= 1726;
    	int option 				= 1;
    	int state  				= 0;

    	char command;
    	char beforeCommand = 'n';

    	userID = new IntByReference(0);

    	ServerSocket serverSoc = null;
    	Socket soc = null;


    	switch (option) {
		case 1:
		{
			if (Edk.INSTANCE.EE_EngineConnect("Emotiv Systems-5") != EdkErrorCode.EDK_OK.ToInt()) {
				System.out.println("Emotiv Engine start up failed.");
				return;
			}
			break;
		}
		case 2:
		{
			System.out.println("Target IP of EmoComposer: [127.0.0.1] ");

			if (Edk.INSTANCE.EE_EngineRemoteConnect("127.0.0.1", composerPort, "Emotiv Systems-5") != EdkErrorCode.EDK_OK.ToInt()) {
				System.out.println("Cannot connect to EmoComposer on [127.0.0.1]");
				return;
			}
			System.out.println("Connected to EmoComposer on [127.0.0.1]");
			break;
		}
		default:
			System.out.println("Invalid option...");
			return;
    	}

    	try {
    		serverSoc = new ServerSocket(PORT);
			System.out.println("Wait...");

			soc = serverSoc.accept();
			System.out.println("Connected: " + soc.getRemoteSocketAddress());

			BufferedWriter out = new BufferedWriter( new OutputStreamWriter( soc.getOutputStream()));
    		while (true) {
    			state = Edk.INSTANCE.EE_EngineGetNextEvent(eEvent);

    			if (state == EdkErrorCode.EDK_OK.ToInt()) {

					int eventType = Edk.INSTANCE.EE_EmoEngineEventGetType(eEvent);
					Edk.INSTANCE.EE_EmoEngineEventGetUserId(eEvent, userID);

					// Log the EmoState if it has been updated
					if (eventType == Edk.EE_Event_t.EE_EmoStateUpdated.ToInt()) {

						Edk.INSTANCE.EE_EmoEngineEventGetEmoState(eEvent, eState);
						float timestamp = EmoState.INSTANCE.ES_GetTimeFromStart(eState);
						System.out.println(timestamp + " : New EmoState from user " + userID.getValue());

						System.out.print("WirelessSignalStatus: ");
						System.out.println(EmoState.INSTANCE.ES_GetWirelessSignalStatus(eState));

						if (EmoState.INSTANCE.ES_ExpressivIsBlink(eState) == 1) {
							command = 'b';
							System.out.println("Blink");
						}
						else if (EmoState.INSTANCE.ES_ExpressivIsLeftWink(eState) == 1) {
							command = 'l';
							System.out.println("LeftWink");
						}
						else if (EmoState.INSTANCE.ES_ExpressivIsRightWink(eState) == 1) {
							command = 'r';
							System.out.println("RightWink");
						}
						else if (EmoState.INSTANCE.ES_ExpressivIsLookingLeft(eState) == 1) {
							command = 'L';
							System.out.println("LookingLeft");
						}
						else if (EmoState.INSTANCE.ES_ExpressivIsLookingRight(eState) == 1) {
							command = 'R';
							System.out.println("LookingRight");
						}
						else if(EmoState.INSTANCE.ES_ExpressivGetLowerFaceActionPower(eState) > 0.5 && EmoState.INSTANCE.ES_ExpressivGetLowerFaceAction(eState) == 256) {
							command = 'c';
							System.out.println("Clench");
						}
						else {
							command = 'n';
							System.out.println("None.");
						}

						if (command != 'n') {
							if (command != beforeCommand) {
								out.write(command);
								out.flush();

							}

						}
						beforeCommand = command;

						//System.out.print("Emostate:: ");
						//System.out.println(EmoState.INSTANCE.ES_ExpressivGetLowerFaceAction(eState)); // 32:eyebrow, 64:furrow, 128:smile, 256:clench
						//System.out.println(EmoState.INSTANCE.ES_ExpressivGetLowerFaceActionPower(eState));


						System.out.print("ExcitementShortTerm: ");
						System.out.println(EmoState.INSTANCE.ES_AffectivGetExcitementShortTermScore(eState));
						System.out.print("ExcitementLongTerm: ");
						System.out.println(EmoState.INSTANCE.ES_AffectivGetExcitementLongTermScore(eState));
						System.out.print("EngagementBoredom: ");
						System.out.println(EmoState.INSTANCE.ES_AffectivGetEngagementBoredomScore(eState));

						System.out.print("CognitivGetCurrentAction: ");
						System.out.println(EmoState.INSTANCE.ES_CognitivGetCurrentAction(eState));
						System.out.print("CurrentActionPower: ");
						System.out.println(EmoState.INSTANCE.ES_CognitivGetCurrentActionPower(eState));
					}
				} // end if
				else if (state != EdkErrorCode.EDK_NO_EVENT.ToInt()) {
					System.out.println("Internal error in Emotiv Engine!");
					break;
				}
    		} // end while
    	} // end try

    	catch (IOException e) {
    		e.printStackTrace();
    	}

    	finally {
    		try {
    			if ( soc != null ) {
    				soc.close();
    				Edk.INSTANCE.EE_EngineDisconnect();
    				Edk.INSTANCE.EE_EmoEngineEventFree(eEvent);
    				System.out.println("Disconnected!");

    			}
    		} // end try
    		catch(IOException e) {}

    		try {
    			if ( serverSoc != null ) {
    				serverSoc.close();
    			}
    		} //end try
    		catch (IOException e) {}
    	} // end finally
    }
}
