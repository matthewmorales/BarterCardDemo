package CatanTradingJade;

import java.util.Hashtable;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class AgentGMTest extends Agent
{
	private AID[] playerAgents;
	private boolean myTurn = false;
	private int turnCount;
	private boolean preTradeDone = false;
	private boolean tradePhaseDone = false;
	private boolean tradeValue = false;
	private AID gameMaster;
	private String targetResource = "Wood";
	private int numRoads = 2, numSettlements = 2, numCities = 0;
	private int currentScore = 2;
	private boolean cityPossible = false, settlementPossible = false;
	private boolean dfPinged = false;
	private boolean tradeAcceptable;
	private String[] WoolRequested, WheatRequested, WoodRequested, StoneRequested, BrickRequested;
	private String cfpContent;
	private int step = 0;
	private int numPlayers;
	int repliesCNT = 0;
	private String cardDistWeight; //Options: EVEN, WOOD, BRICK, STONE, WHEAT, WOOL
	private String tradingStrat;	//Options: EQUALS, GREATER, NONE, UNSURE

	private Hashtable<String, Integer> currentHand;
	private Hashtable<String, Integer> propTradeHash;
	private Hashtable<String, Integer> receivedPropTradeHash;

	public String handToString(Hashtable<String, Integer> hand) 
	{
	    StringBuilder handString = new StringBuilder();
		handString.append("Wool:");
		handString.append(hand.get("Wool"));
		handString.append(",");
		handString.append("Wheat:");
		handString.append(hand.get("Wheat"));
		handString.append(",");
		handString.append("Stone:");
		handString.append(hand.get("Stone"));
		handString.append(",");
		handString.append("Wood:");
		handString.append(hand.get("Wood"));
		handString.append(",");
		handString.append("Brick:");
		handString.append(hand.get("Brick"));
		return handString.toString();
	}	
	
	public boolean acceptProposedTrade(Hashtable<String, Integer> currentHand, Hashtable<String, Integer> proposedTrade) 
	{
		if(tradingStrat.equals("EQUALS"))
		{
			Integer currentHandValue, proposedHandValue = 0;
			currentHandValue = getHandValue(currentHand);
			proposedHandValue = getHandValue(proposedTrade);
			return proposedHandValue >= currentHandValue;
		}
		
		else if(tradingStrat.equals("GREATER"))
		{
			Integer currentHandValue, proposedHandValue = 0;
			currentHandValue = getHandValue(currentHand);
			proposedHandValue = getHandValue(proposedTrade);
			return proposedHandValue > currentHandValue;
		}
		
		else if(tradingStrat.equals("NONE"))
		{
			Integer currentHandValue, proposedHandValue = 0;
			currentHandValue = getHandValue(currentHand);
			proposedHandValue = getHandValue(proposedTrade);
			return false;
		}
		
		else if(tradingStrat.equals("UNSURE"))
		{
			double decider = Math.random();
			if(decider <= 0.75)
			{
				Integer currentHandValue, proposedHandValue = 0;
				currentHandValue = getHandValue(currentHand);
				proposedHandValue = getHandValue(proposedTrade);
				return proposedHandValue >= currentHandValue;
			}
			else
			{
				return false;
			}
		}
		else
		{
			Integer currentHandValue, proposedHandValue = 0;
			currentHandValue = getHandValue(currentHand);
			proposedHandValue = getHandValue(proposedTrade);
			return proposedHandValue >= currentHandValue;
		}
	}
		
	public Integer getHandValue(Hashtable<String, Integer> hand) 
	{
		Integer handValue = 0, woodUsed = 0, woolUsed = 0, stoneUsed = 0, wheatUsed = 0, brickUsed = 0, remainingCardsValue = 0;
		tradeValue = false;
		
		// Check for completed structures in order of point value: City, Settlement, Road
		
		// City = 2 wheat, 3 stone
		if (hand.get("Wheat") - wheatUsed >= 2 && hand.get("Stone") - stoneUsed >= 3
				&& wheatUsed <= hand.get("Wheat") && stoneUsed <= hand.get("Stone")
				&& cityPossible) 
		{
			handValue += 6;
			wheatUsed += 2;
			stoneUsed += 3;
		}
		// Settlement = 1 wheat, 1 brick, 1 wood, 1 wool
		if (hand.get("Wheat") - wheatUsed >= 1 && hand.get("Brick") - brickUsed >= 1
			    && hand.get("Wood") - woodUsed >= 1 && hand.get("Wool") - woolUsed >= 1
			    && wheatUsed <= hand.get("Wheat") && woodUsed <= hand.get("Wood") 
			    && woolUsed <= hand.get("Wool") && brickUsed <= hand.get("Brick") 
			    && settlementPossible) 
		{
			handValue += 5;
			wheatUsed += 1;
			brickUsed += 1;
			woodUsed += 1;
			woolUsed += 1;
		}
		// Road = 1 brick, 1 wood
		if (hand.get("Brick") - brickUsed >= 1 && hand.get("Wood") - woodUsed >= 1
				&& woodUsed <= hand.get("Wood")	&& brickUsed <= hand.get("Brick")) 
		{
			handValue += 3;
			brickUsed += 1;
			woodUsed += 1;
		}
		
		// Check what you're closest to building next
		
		// City missing one wheat or one stone
		if (hand.get("Wheat") - wheatUsed == 1 && hand.get("Stone") - stoneUsed >= 3
				&& wheatUsed <= hand.get("Wheat") && stoneUsed <= hand.get("Stone") && cityPossible) 
		{
			handValue += 4;
			wheatUsed += 1;
			stoneUsed += 3;
			targetResource = "Wheat";
			tradeValue = true;
		}
		if (hand.get("Wheat") - wheatUsed >= 2 && hand.get("Stone") - stoneUsed == 2
				&& wheatUsed <= hand.get("Wheat") && stoneUsed <= hand.get("Stone") && cityPossible) 
		{
			handValue += 4;
			wheatUsed += 2;
			stoneUsed += 2;
			targetResource = "Stone";
			tradeValue = true;
		}
		
		// Settlement missing 1 wheat, 1 brick, 1 wood, or 1 wool
		if (hand.get("Wheat") - wheatUsed == 0 && hand.get("Brick") - brickUsed >= 1
			&& hand.get("Wood") - woodUsed >= 1 && hand.get("Wool") - woolUsed >= 1
			&& wheatUsed <= hand.get("Wheat") && woodUsed <= hand.get("Wood") 
			&& woolUsed <= hand.get("Wool") && brickUsed <= hand.get("Brick")
			&& settlementPossible) 
		{
			handValue += 3;
			brickUsed += 1;
			woodUsed += 1;
			woolUsed += 1;
			targetResource = "Wheat";
			tradeValue = true;
		}
		if (hand.get("Wheat") - wheatUsed >= 1 && hand.get("Brick") - brickUsed == 0
			&& hand.get("Wood") - woodUsed >= 1 && hand.get("Wool") - woolUsed >= 1
			&& wheatUsed <= hand.get("Wheat") && woodUsed <= hand.get("Wood") 
			&& woolUsed <= hand.get("Wool") && brickUsed <= hand.get("Brick")
			&& settlementPossible) 
		{
			handValue += 3;
			wheatUsed += 1;
			woodUsed += 1;
			woolUsed += 1;
			targetResource = "Brick";
			tradeValue = true;
		}
		if (hand.get("Wheat") - wheatUsed >= 1 && hand.get("Brick") - brickUsed >= 1
			&& hand.get("Wood") - woodUsed == 0 && hand.get("Wool") - woolUsed >= 1
			&& wheatUsed <= hand.get("Wheat") && woodUsed <= hand.get("Wood") 
			&& woolUsed <= hand.get("Wool") && brickUsed <= hand.get("Brick")
			&& settlementPossible) 
		{
			handValue += 3;
			wheatUsed += 1;
			brickUsed += 1;
			woolUsed += 1;
			targetResource = "Wood";
			tradeValue = true;
		}
		if (hand.get("Wheat") - wheatUsed >= 1 && hand.get("Brick") - brickUsed >= 1
			&& hand.get("Wood") - woodUsed >= 1 && hand.get("Wool") - woolUsed == 0
			&& wheatUsed <= hand.get("Wheat") && woodUsed <= hand.get("Wood") 
			&& woolUsed <= hand.get("Wool") && brickUsed <= hand.get("Brick")
			&& settlementPossible) 
		{
			handValue += 3;
			wheatUsed += 1;
			brickUsed += 1;
			woodUsed += 1;
			targetResource = "Wool";
			tradeValue = true;
		}
		
		// Road missing 1 brick or 1 wood
		if (hand.get("Brick") - brickUsed == 0 && hand.get("Wood") - woodUsed >= 1
				&& woodUsed <= hand.get("Wood") && brickUsed <= hand.get("Brick")) 
		{
			handValue += 1;
			woodUsed += 1;
			targetResource = "Brick";
			tradeValue = true;
		}
		if (hand.get("Brick") - brickUsed >= 1 && hand.get("Wood") - woodUsed == 0
				&& woodUsed <= hand.get("Wood") && brickUsed <= hand.get("Brick")) 
		{
			handValue += 1;
			brickUsed += 1;
			targetResource = "Wood";
			tradeValue = true;
		}
		
		if(wheatUsed <= hand.get("Wheat") || woodUsed <= hand.get("Wood") || brickUsed <= hand.get("Brick")
				|| stoneUsed <= hand.get("Stone") || woolUsed <= hand.get("Wool"))
		{
			remainingCardsValue = (hand.get("Wheat") - wheatUsed) + (hand.get("Wood") - woodUsed)
					+ (hand.get("Wool") - woolUsed) + (hand.get("Stone") - stoneUsed) + (hand.get("Brick") - brickUsed);
			handValue = handValue + remainingCardsValue;
		}
		
		return handValue;
	}

	public void generateReceivedProposal(String messageContent, Hashtable<String, Integer> hand)
	{
		String[] receivedProposedTrade;
		
		receivedPropTradeHash.put("Wool", 0);
		receivedPropTradeHash.put("Wood", 0);
		receivedPropTradeHash.put("Stone", 0);
		receivedPropTradeHash.put("Brick", 0);
		receivedPropTradeHash.put("Wheat", 0);
		
		receivedProposedTrade = messageContent.split(",");
		WoolRequested = receivedProposedTrade[0].split(":");
		WheatRequested = receivedProposedTrade[1].split(":");
		StoneRequested = receivedProposedTrade[2].split(":");
		WoodRequested = receivedProposedTrade[3].split(":");
		BrickRequested = receivedProposedTrade[4].split(":");
		
		receivedPropTradeHash.put("Wool", hand.get("Wool") - Integer.parseInt(WoolRequested[1]));
		receivedPropTradeHash.put("Wheat", hand.get("Wheat") - Integer.parseInt(WheatRequested[1]));
		receivedPropTradeHash.put("Stone", hand.get("Stone") - Integer.parseInt(StoneRequested[1]));
		receivedPropTradeHash.put("Wood", hand.get("Wood") - Integer.parseInt(WoodRequested[1]));
		receivedPropTradeHash.put("Brick", hand.get("Brick") - Integer.parseInt(BrickRequested[1]));
		receivedPropTradeHash.put(targetResource, currentHand.get(targetResource) + 1);
	}
	
	public void buildStructures()
	{
	    if (numSettlements >=1 && currentHand.get("Wheat") >= 2 && currentHand.get("Stone") >= 3)
	    {
	        System.out.println("Building City");
	        numCities = numCities + 1;
	        numSettlements = numSettlements - 1;
	        currentHand.put("Wheat", currentHand.get("Wheat") - 2);
	        currentHand.put("Stone", currentHand.get("Stone") - 3);
	    }
	    else if((numSettlements + numCities) * 2 <= numRoads 
	    && currentHand.get("Wheat") >= 1 && currentHand.get("Wood") >= 1
	    && currentHand.get("Wool") >= 1 && currentHand.get("Brick") >= 1)
	    {
	        System.out.println("Building Settlement");
	        numSettlements = numSettlements + 1;
	        currentHand.put("Wheat", currentHand.get("Wheat") - 1);
	        currentHand.put("Wood", currentHand.get("Wood") - 1);
	        currentHand.put("Wool", currentHand.get("Wool") - 1);
	        currentHand.put("Brick", currentHand.get("Brick") - 1);
	    }
	    else if(numRoads < ((numSettlements + numCities) * 2) 
	    && currentHand.get("Wood") >= 1 && currentHand.get("Brick") >= 1)
	    {
	        System.out.println("Building Road");
	        numRoads = numRoads + 1;
	        currentHand.put("Wood", currentHand.get("Wood") - 1);
	        currentHand.put("Brick", currentHand.get("Brick") - 1); 
	    }
	    
	    if(numSettlements >= 1)
	    {
	        cityPossible = true;
	    }
	    else
	    {
	        cityPossible = false;
	    }
	    
	    if((numSettlements + numCities) == 2*numRoads)
	    {
	        settlementPossible = true;
	    }
	    else
	    {
	        settlementPossible = false;
	    }
	}
	
	public String generateProposal()
	{
	    propTradeHash.put("Wool", 0);
		propTradeHash.put("Wheat", 0);
		propTradeHash.put("Stone", 0);
		propTradeHash.put("Wood", 0);
		propTradeHash.put("Brick", 0);
		propTradeHash.put(targetResource, 1);
		
		StringBuilder desiredTrade = new StringBuilder();
		desiredTrade.append("Wool:");
		desiredTrade.append(propTradeHash.get("Wool"));
		desiredTrade.append(",");
		desiredTrade.append("Wheat:");
		desiredTrade.append(propTradeHash.get("Wheat"));
		desiredTrade.append(",");
		desiredTrade.append("Stone:");
		desiredTrade.append(propTradeHash.get("Stone"));
		desiredTrade.append(",");
		desiredTrade.append("Wood:");
		desiredTrade.append(propTradeHash.get("Wood"));
		desiredTrade.append(",");
		desiredTrade.append("Brick:");
		desiredTrade.append(propTradeHash.get("Brick"));
		
		return desiredTrade.toString();
	}
	
	public void cardDistribution(int victoryPoints)
	{
		int cardsGenerated = 4;
		
		if(victoryPoints < 5)
		{
			cardsGenerated = 4;
		}
		
		else if(victoryPoints < 10)
		{
			cardsGenerated = 5;
		}
		if(cardDistWeight.equals("WOOD"))
		{
			for(int i = 0; i < cardsGenerated; i++)
			{
				int cardPick = (int) (Math.random() * 99) + 1;
				
				if(cardPick < 15)
				{
					currentHand.put("Wool", currentHand.get("Wool") + 1);
					System.out.println("Gained 1 Wool");
				}
				else if(cardPick < 43)
				{
					currentHand.put("Wood", currentHand.get("Wood") + 1);
					System.out.println("Gained 1 Wood");
				}
				else if(cardPick < 57)
				{
					currentHand.put("Wheat", currentHand.get("Wheat") + 1);
					System.out.println("Gained 1 Wheat");
				}
				else if(cardPick < 71)
				{
					currentHand.put("Stone", currentHand.get("Stone") + 1);
					System.out.println("Gained 1 Stone");
				}
				else if(cardPick < 85)
				{
					currentHand.put("Brick", currentHand.get("Brick") + 1);
					System.out.println("Gained 1 Brick");
				}
				else if(cardPick < 101)
				{
					System.out.println("No Card Generated");
				}
			}
		}
		
		if(cardDistWeight.equals("WHEAT"))
		{
			for(int i = 0; i < cardsGenerated; i++)
			{
				int cardPick = (int) (Math.random() * 99) + 1;
				
				if(cardPick < 15)
				{
					currentHand.put("Wool", currentHand.get("Wool") + 1);
					System.out.println("Gained 1 Wool");
				}
				else if(cardPick < 43)
				{
					currentHand.put("Wheat", currentHand.get("Wheat") + 1);
					System.out.println("Gained 1 Wheat");
				}
				else if(cardPick < 57)
				{
					currentHand.put("Wood", currentHand.get("Wood") + 1);
					System.out.println("Gained 1 Wood");
				}
				else if(cardPick < 71)
				{
					currentHand.put("Stone", currentHand.get("Stone") + 1);
					System.out.println("Gained 1 Stone");
				}
				else if(cardPick < 85)
				{
					currentHand.put("Brick", currentHand.get("Brick") + 1);
					System.out.println("Gained 1 Brick");
				}
				else if(cardPick < 101)
				{
					System.out.println("No Card Generated");
				}
			}
		}
		
		if(cardDistWeight.equals("WOOL"))
		{
			for(int i = 0; i < cardsGenerated; i++)
			{
				int cardPick = (int) (Math.random() * 99) + 1;
				
				if(cardPick < 15)
				{
					currentHand.put("Wood", currentHand.get("Wood") + 1);
					System.out.println("Gained 1 Wood");
				}
				else if(cardPick < 43)
				{
					currentHand.put("Wool", currentHand.get("Wool") + 1);
					System.out.println("Gained 1 Wool");
				}
				else if(cardPick < 57)
				{
					currentHand.put("Wheat", currentHand.get("Wheat") + 1);
					System.out.println("Gained 1 Wheat");
				}
				else if(cardPick < 71)
				{
					currentHand.put("Stone", currentHand.get("Stone") + 1);
					System.out.println("Gained 1 Stone");
				}
				else if(cardPick < 85)
				{
					currentHand.put("Brick", currentHand.get("Brick") + 1);
					System.out.println("Gained 1 Brick");
				}
				else if(cardPick < 101)
				{
					System.out.println("No Card Generated");
				}
			}
		}
		
		if(cardDistWeight.equals("STONE"))
		{
			for(int i = 0; i < cardsGenerated; i++)
			{
				int cardPick = (int) (Math.random() * 99) + 1;
				
				if(cardPick < 15)
				{
					currentHand.put("Wool", currentHand.get("Wool") + 1);
					System.out.println("Gained 1 Wool");
				}
				else if(cardPick < 43)
				{
					currentHand.put("Stone", currentHand.get("Stone") + 1);
					System.out.println("Gained 1 Stone");
				}
				else if(cardPick < 57)
				{
					currentHand.put("Wheat", currentHand.get("Wheat") + 1);
					System.out.println("Gained 1 Wheat");
				}
				else if(cardPick < 71)
				{
					currentHand.put("Wood", currentHand.get("Wood") + 1);
					System.out.println("Gained 1 Wood");
				}
				else if(cardPick < 85)
				{
					currentHand.put("Brick", currentHand.get("Brick") + 1);
					System.out.println("Gained 1 Brick");
				}
				else if(cardPick < 101)
				{
					System.out.println("No Card Generated");
				}
			}
		}
		
		if(cardDistWeight.equals("BRICK"))
		{
			for(int i = 0; i < cardsGenerated; i++)
			{
				int cardPick = (int) (Math.random() * 99) + 1;
				
				if(cardPick < 15)
				{
					currentHand.put("Wool", currentHand.get("Wool") + 1);
					System.out.println("Gained 1 Wool");
				}
				else if(cardPick < 43)
				{
					currentHand.put("Brick", currentHand.get("Brick") + 1);
					System.out.println("Gained 1 Brick");
				}
				else if(cardPick < 57)
				{
					currentHand.put("Wheat", currentHand.get("Wheat") + 1);
					System.out.println("Gained 1 Wheat");
				}
				else if(cardPick < 71)
				{
					currentHand.put("Stone", currentHand.get("Stone") + 1);
					System.out.println("Gained 1 Stone");
				}
				else if(cardPick < 85)
				{
					currentHand.put("Wood", currentHand.get("Wood") + 1);
					System.out.println("Gained 1 Wood");
				}
				else if(cardPick < 101)
				{
					System.out.println("No Card Generated");
				}
			}
		}
		
		if(cardDistWeight.equals("EVEN") || cardDistWeight.equals(null))
		{
			for(int i = 0; i < cardsGenerated; i++)
			{
				int cardPick = (int) (Math.random() * 99) + 1;
				
				if(cardPick < 17)
				{
					currentHand.put("Wool", currentHand.get("Wool") + 1);
					System.out.println("Gained 1 Wool");
				}
				else if(cardPick < 34)
				{
					currentHand.put("Brick", currentHand.get("Brick") + 1);
					System.out.println("Gained 1 Brick");
				}
				else if(cardPick < 51)
				{
					currentHand.put("Wheat", currentHand.get("Wheat") + 1);
					System.out.println("Gained 1 Wheat");
				}
				else if(cardPick < 69)
				{
					currentHand.put("Stone", currentHand.get("Stone") + 1);
					System.out.println("Gained 1 Stone");
				}
				else if(cardPick < 86)
				{
					currentHand.put("Wood", currentHand.get("Wood") + 1);
					System.out.println("Gained 1 Wood");
				}
				else if(cardPick < 101)
				{
					System.out.println("No Card Generated");
				}
			}
		}
		
	}
	
	protected void setup()
	{
		Object[] args = getArguments();
		if(args != null && args.length > 0)
		{
			cardDistWeight = (String) args[0];
			tradingStrat = (String) args[1];
		}
		
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Catan");
		sd.setName("CatanTrading");
		dfd.setName(getAID());
		dfd.addServices(sd);
		System.out.println("Registered");
		System.out.println("Card Distribution is " + cardDistWeight);
		System.out.println("Trading Strategy is " + tradingStrat);
		
		try
		{
			DFService.register(this,  dfd);
		}
		catch (FIPAException fe)
		{
			fe.printStackTrace();
		}
		
		currentHand = new Hashtable<String, Integer>();
		currentHand.put("Wool", 0);
		currentHand.put("Wheat", 0);
		currentHand.put("Stone", 0);
		currentHand.put("Wood", 0);
		currentHand.put("Brick", 0);
		
		propTradeHash = new Hashtable<String, Integer>();
		propTradeHash.put("Wool", 0);
		propTradeHash.put("Wheat", 0);
		propTradeHash.put("Stone", 0);
		propTradeHash.put("Wood", 0);
		propTradeHash.put("Brick", 0);
		
		receivedPropTradeHash = new Hashtable<String, Integer>();
		receivedPropTradeHash.put("Wool", 0);
		receivedPropTradeHash.put("Wood", 0);
		receivedPropTradeHash.put("Stone", 0);
		receivedPropTradeHash.put("Brick", 0);
		receivedPropTradeHash.put("Wheat", 0);
		
		addBehaviour(new receiveMyTurn());
		addBehaviour(new preTrade());
		addBehaviour(new TradePhase());
		addBehaviour(new acceptTradeMessages());
		
		
	}
	
	private class receiveMyTurn extends CyclicBehaviour
	{
		private MessageTemplate mt;
		
		public void action() 
		{
			mt = MessageTemplate.MatchConversationId("Turn");
			ACLMessage gmMessage = myAgent.receive(mt);
			if(myTurn == false && gmMessage != null)
			{
				gameMaster = gmMessage.getSender();
				if(gmMessage.getPerformative() == ACLMessage.INFORM)
				{
					myTurn = true;
					preTradeDone = false;
					tradeValue = false;
					tradePhaseDone = false;
					
					System.out.println("It is now My Turn");
				}
				
				if(dfPinged == false)
				{
			    	DFAgentDescription template = new DFAgentDescription();
			        ServiceDescription sd = new ServiceDescription();
			        sd.setType("Catan");
			        template.addServices(sd);
		    	    try 
		    	    {
				        DFAgentDescription[] result = DFService.search(myAgent, template); 
				        System.out.println("Found the following player agents:" + result.length);
				        playerAgents = new AID[result.length];
				        numPlayers = result.length;
				        for (int i = 0; i < result.length; ++i) 
				        {
					        playerAgents[i] = result[i].getName();
				    	    System.out.println(playerAgents[i].getName());
			    	    }
			    	    dfPinged = true;
		       	    }
			        catch (FIPAException fe) 
			        {
				        fe.printStackTrace();
			        }
		    	}
			}	
		}	
	}
	
	private class preTrade extends CyclicBehaviour
	{

		@Override
		public void action() {
			// TODO Auto-generated method stub
			if(myTurn == true && preTradeDone == false)
			{
				currentScore = (numCities * 2) + numSettlements;
			    //distribute cards here
				cardDistribution(currentScore);
				buildStructures();
				getHandValue(currentHand);
				//Check Hand for trade options
				//set target resource and tradeValue here
				System.out.println("Current Hand is: " + handToString(currentHand));
				System.out.println("Pretrade Actions Complete");
				preTradeDone = true;
			}
			
			if(myTurn == true && preTradeDone == true && tradeValue == false)
			{
				System.out.println("No value to trading");
				tradePhaseDone = true;
			}
			
		}	
	}
	
	private class TradePhase extends CyclicBehaviour
	{
		private MessageTemplate mt;
		private AID tradingPlayer;
		
		@Override
		public void action() 
		{
			// TODO Auto-generated method stub
			if(myTurn == true && preTradeDone == true && tradePhaseDone == false && tradeValue == true)
			{
				//Trade here. Make Decisions about trade values etc...
				//CFP Goes Here
				
				switch(step)
				{
					case 0:
						System.out.println("*************");
						System.out.println("Trying to TRADE");
						System.out.println("Current Hand: " + handToString(currentHand));
						// TODO Auto-generated method stub
						ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
						for (int i = 0; i < playerAgents.length; ++i) 
						{
							cfp.addReceiver(playerAgents[i]);
						} 
						// Creating a standard message of the form: Wool:0,Wheat:0,Stone:0,Wood:0,Brick:0
						cfpContent = generateProposal();			
						System.out.println("Target Resources: " + cfpContent);
						cfp.setContent(cfpContent);
						cfp.setConversationId("Catan-Trading");
						send(cfp);
						mt = MessageTemplate.MatchConversationId("Catan-Trading");
						step = 1;
						break;
				case 1:
						// Receive all proposals/refusals from seller agents
						ACLMessage response = myAgent.receive(mt);
						if (response != null) 
						{
							// Reply received
							//System.out.println(response);
							if (response.getPerformative() == ACLMessage.PROPOSE) 
							{
								// This is an offer 
								generateReceivedProposal(response.getContent(), currentHand);
								System.out.println("Other Player is Requesting: " + response.getContent());
								System.out.println("Current Hand Value: " + getHandValue(currentHand));
								System.out.println("Proposed Hand Value: " + getHandValue(receivedPropTradeHash));
								if (acceptProposedTrade(currentHand, receivedPropTradeHash)) 
								{
									// This is the best offer at present
									tradingPlayer = response.getSender();
									System.out.println("Player " + response.getSender().getName() + " has offered " + response.getContent());
									//tradeAcceptable = true;
								}
							}
							repliesCNT++;
							if (repliesCNT >= numPlayers) 
							{
								// We received all replies
								step = 2; 
							}
						}
						else 
						{
							block();
						}
						break;
					case 2:
						// Send the purchase order to the seller that provided the best offer
						ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
						order.addReceiver(tradingPlayer);
						order.setContent(cfpContent);
						order.setConversationId("Catan-Trading");
						myAgent.send(order);

						// Prepare the template to get the purchase order reply
						mt = MessageTemplate.MatchConversationId("Catan-Trading");
						step = 3;
						break;
					case 3:      
						// Receive the purchase order reply
						response = myAgent.receive(mt);
						if (response != null) 
						{
							// Purchase order reply received
							if (response.getPerformative() == ACLMessage.CONFIRM) ////////////////////Change Inform
							{
								System.out.println(targetResource
										+ " successfully traded with player "
										+ response.getSender().getName());
								
								System.out.println("Trade Phase is done");
								currentHand.put("Wood", receivedPropTradeHash.get("Wood"));
								currentHand.put("Wool", receivedPropTradeHash.get("Wool"));
								currentHand.put("Wheat", receivedPropTradeHash.get("Wheat"));
								currentHand.put("Stone", receivedPropTradeHash.get("Stone"));
								currentHand.put("Brick", receivedPropTradeHash.get("Brick"));
								System.out.println("Post Trade Hand: " + handToString(currentHand));
								tradePhaseDone = true;
								repliesCNT = 0;
								step = 0;
								tradingPlayer = null;
								
								try {
									Thread.sleep(200);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								
								break;
							}
							else 
							{
								System.out.println("Attempt failed: requested book already sold.");
							}
						}
						else 
						{
							block();
						}
						break;		
				}
				if (step == 2 && tradingPlayer == null) 
				{
					System.out.println("Attempt failed: "+ targetResource +" not available for trade. Replies CNT = " + repliesCNT);
					tradePhaseDone = true;
					System.out.println("Trade Phase is Done");
					repliesCNT = 0;
					step = 0;
					tradingPlayer = null;
				}	
			}
			
			if(myTurn == true && tradePhaseDone == true)
			{
				//re-assess and possibly build more here.
				myTurn = false;
				buildStructures();
				
				currentScore = (numCities*2) + numSettlements;
				
				System.out.println("Current Number of Roads = " + numRoads);
				System.out.println("Current Number of Settlements = " + numSettlements);
				System.out.println("Current Number of Cities = " +numCities);
				System.out.println("Turn Is Complete");
				
				System.out.println("");
				System.out.println("");
				//RESET ALL BOOLEANS HERE TO END TURN FO REAL
				myTurn = false;
				tradePhaseDone = false;
				tradeValue = false;
				preTradeDone = false;
				tradingPlayer = null;
				step = 0;
						
				if(currentScore >= 10)
				{
					ACLMessage turnComplete = new ACLMessage(ACLMessage.INFORM);
					turnComplete.addReceiver(gameMaster);
					turnComplete.setContent("Victory");
					send(turnComplete);	
					System.out.println("***************");
					System.out.println("Victory");
					System.out.println("***************");
				}
				else
				{
					ACLMessage turnComplete = new ACLMessage(ACLMessage.INFORM);
					turnComplete.addReceiver(gameMaster);
					turnComplete.setContent("Turn Complete");
					send(turnComplete);
				}
				
				
			}
		}	
	}

	private class acceptTradeMessages extends CyclicBehaviour
	{
		private MessageTemplate mt;
		
		@Override
		public void action() {
			// TODO Auto-generated method stub
			if(myTurn == false)
			{
				mt = MessageTemplate.MatchConversationId("Catan-Trading");
				//logic for receiving CFP & Responding.
				ACLMessage cfpReceived = myAgent.receive(mt);
				if(cfpReceived != null)
				{
					getHandValue(currentHand);
					if(cfpReceived.getPerformative() == ACLMessage.CFP)
					{
						System.out.println("CFP Received from " + cfpReceived.getSender().getName());
						System.out.println("Current Hand: " + handToString(currentHand));
						System.out.println(cfpReceived.getSender().getName() + " is requesting " + cfpReceived.getContent());
						generateReceivedProposal(cfpReceived.getContent(), currentHand);
						if(acceptProposedTrade(currentHand, receivedPropTradeHash) && !generateProposal().equals(cfpReceived.getContent()))
						{
							tradeAcceptable = true;
							System.out.println("CFP Accepted");
							ACLMessage proposeTrade = new ACLMessage(ACLMessage.PROPOSE);
							proposeTrade.setPerformative(ACLMessage.PROPOSE);
							proposeTrade.setContent(generateProposal());
							proposeTrade.addReceiver(cfpReceived.getSender());
							proposeTrade.setConversationId("Catan-Trading");
							send(proposeTrade);
							System.out.println("Proposal Sent: " + proposeTrade.getContent());
						}
						else
						{
							System.out.println("CFP Rejected");
							ACLMessage refuseTrade = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
							refuseTrade.setContent("REJECTED");
							refuseTrade.addReceiver(cfpReceived.getSender());
							refuseTrade.setConversationId("Catan-Trading");
							send(refuseTrade);
						}
					}
					if(cfpReceived.getPerformative() == ACLMessage.ACCEPT_PROPOSAL && tradeAcceptable)
					{
						ACLMessage orderEscrow = new ACLMessage(ACLMessage.CONFIRM);
						orderEscrow.setPerformative(ACLMessage.CONFIRM);
						orderEscrow.setContent("Trade Accepted");
						orderEscrow.addReceiver(cfpReceived.getSender());
						orderEscrow.setConversationId("Catan-Trading");
						send(orderEscrow);
						
						currentHand.put("Wood", receivedPropTradeHash.get("Wood"));
						currentHand.put("Wool", receivedPropTradeHash.get("Wool"));
						currentHand.put("Wheat", receivedPropTradeHash.get("Wheat"));
						currentHand.put("Stone", receivedPropTradeHash.get("Stone"));
						currentHand.put("Brick", receivedPropTradeHash.get("Brick"));
						System.out.println("Post Trade Hand: " + handToString(currentHand));
						
						tradeAcceptable = false;
					}
				}
			}
		}
	}
}