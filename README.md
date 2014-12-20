This is a simple trading program created as a practice using JADE to develop multi-agent systems.
It crudely emulates the popular board game "Settlers of Catan" and has been designed to allow the
inclusion of multiple strategies as well as different card distribution weights.

The jade framework (downloads, tutorials, documentation, etc) can be found here: http://jade.tilab.com/

To run the program(s) first you must run the jade environment, followed by the agents you wish to participate
in the game (you can have as many participants as you wish) and finally launch the gamemaster agent. The gamemaster
agent will identify all participants and start the game as soon as it is initialized.

Example arguments for initializing player agents:

-container EvenEquals:CatanTradingJade.AgentGMTest(EVEN,EQUALS)
-container EvenGreater:CatanTradingJade.AgentGMTest(EVEN,GREATER)
-container EvenNone:CatanTradingJade.AgentGMTest(EVEN,NONE)
-container EvenUnsure:CatanTradingJade.AgentGMTest(EVEN,UNSURE)

The "EVEN" arguments correspont to an equal chance for producing each resource or no card at all (roughly 16% per option)
The EQUALS, GREATER, NONE, and UNSURE arguments are used to establish an agent strategy at runtime.

In its current state, this example can best be viewed as a skeleton from which an autonomous game of Catan could be made.
As time permits, I hope to implement the following changes:

1) More realistic trading strategies, such as a strategy where agents will not trade with the current winner, or an agent
  who previously refused to trade.
2) More true-to-Catan card distribution. As it currently stands, agents generate cards in a manner not consistent with
  the actual game in two ways: they only generate cards on their turn, and relative probability of generating certain resource
  cards never changes. I would like to implement the means for agents to generate cards on every player's turn (like the real
  game) as well implement a means to distribute cards more in-line with the game.
3) GUI and minor accompanying changes.
4) Missing game mechanics (actual dice rolls, robber, armys, longest road, etc.)
