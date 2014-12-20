package CatanTradingJade;

//import CatanTradingJade.PlayerAgent1.RequestPerformer;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class GameMaster extends Agent
{
	
	private AID[] PlayerAgents;
	private int numPlayers;
	private boolean turnSent = false;
	int agentTurnTicker = 0;
	private int turnCounter = 0;
	
	protected void setup()
	{
		addBehaviour(new InitPlayerAgents());
		addBehaviour(new cycleTurns());
	}
	private class InitPlayerAgents extends OneShotBehaviour 
	{

		@Override
		public void action() 
		{
			// TODO Auto-generated method stub
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("Catan");
			template.addServices(sd);
			try {
				DFAgentDescription[] result = DFService.search(myAgent, template); 
				System.out.println("Found the following player agents:" + result.length);
				PlayerAgents = new AID[result.length];
				numPlayers = result.length;
				for (int i = 0; i < result.length; ++i) {
					PlayerAgents[i] = result[i].getName();
					System.out.println(PlayerAgents[i].getName());
				}
			}
			catch (FIPAException fe) {
				fe.printStackTrace();
			}
			System.out.println("Finished Pinging DF");
			System.out.println("Starting Game!");
			ACLMessage sendTurn = new ACLMessage(ACLMessage.INFORM);
			sendTurn.setContent("Player Agent " + PlayerAgents[agentTurnTicker % numPlayers].getName() + "'s turn");
			sendTurn.addReceiver(PlayerAgents[agentTurnTicker % numPlayers]);
			sendTurn.setConversationId("Turn");
			System.out.println("Player Agent " + PlayerAgents[agentTurnTicker % numPlayers].getName() + "'s turn");
			send(sendTurn);
			agentTurnTicker = agentTurnTicker + 1;
			turnSent = true;
		}
		
	}
	
	private class cycleTurns extends CyclicBehaviour
	{
		BufferedWriter writer = null;
		
		public void action() 
		{
			try 
			{
				Thread.sleep(10);
			} 
			catch (InterruptedException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ACLMessage sendTurn = new ACLMessage(ACLMessage.INFORM);
			ACLMessage agentTurnCompleted = myAgent.receive();
			if(turnSent == false)
			{
				System.out.println("Player Agent " + PlayerAgents[agentTurnTicker % numPlayers].getName() + "'s turn");
				sendTurn.setContent("Player Agent " + PlayerAgents[agentTurnTicker % numPlayers].getName() + "'s turn");
				sendTurn.addReceiver(PlayerAgents[agentTurnTicker % numPlayers]);
				sendTurn.setConversationId("Turn");
				send(sendTurn);
				turnSent = true;
				agentTurnTicker = agentTurnTicker + 1;
				if(agentTurnTicker % numPlayers == 0)
				{
					turnCounter = turnCounter + 1;
				}
			}
			if(turnSent == true && agentTurnCompleted != null)
			{
				System.out.println(agentTurnCompleted.getContent());
				if(agentTurnCompleted.getContent().equals("Turn Complete"))
				{
					turnSent = false;
					System.out.println("Player Agent " + agentTurnCompleted.getSender().getName() + " has completed their turn");
				}
				if(agentTurnCompleted.getContent().equals("Victory"))
				{
					System.out.println("");
					System.out.println("*************************************");
					System.out.println("Player Agent " + agentTurnCompleted.getSender().getName() + " has WON!");
					System.out.println("*************************************");
					System.out.println("");
					
					try 
			        {
			            //create a temporary file
			            String timeLog = new SimpleDateFormat("yyyyMMdd").format(Calendar.getInstance().getTime());
			            File logFile = new File(timeLog);

			            // This will output the full path where the file will be written to...
			            System.out.println("Log file written at: ");
			            System.out.println(logFile.getCanonicalPath());

			            writer = new BufferedWriter(new FileWriter(logFile, true));
			            writer.write("Player Agent " + agentTurnCompleted.getSender().getName() + " has WON, after " + turnCounter + " turns!" + "\r" + "\n");
			        } 
			        catch (Exception e) 
			        {
			            e.printStackTrace();
			        } 
			        finally 
			        {
			            try 
			            {
			            	// Close the writer regardless of what happens...
			                writer.close();
			            } 
			            catch (Exception e) 
			            {
			            
			            }
			        }
					
					doDelete();
				}
			}
			
		}
		
	}

}
