# snigilbot

<img src="https://github.com/downloads/hyPiRion/snigilbot/snigil.png"
 alt="snigilbot logo" title="The snail himself" align="right" />

snigilbot is a quarto simulator along with different agent opponents. It
utilizes minimax with alpha-beta pruning to evaluate the next move. The name
comes from its unique ability to evaluate state much slower than any of the
C/Java-made minimax agents it has currently played against.
<br/><br/><br/><br/>

## Installation

Installation is straight out of the box: Get a Java version higher than 1.6 and
you're set. If you want to compile the program yourself, you need to install
[Leiningen 2][lein] and run `lein uberjar` in this folder. This will generate a
`snigil.jar`-file which will be available for use.

## Usage

It is possible to use snigilbot as both a tournament agent and as a 1-vs-1
simulator. You run snigilbot by calling the following command:

```sh
$ java -jar snigil.jar bot1 [opt-arg] bot2 [opt-arg]
```

Where `bot1` and `bot2` is the name of the different bots you want to play: You
can either choose "minimax", "random", "novice" or "human". For minimax, an
integer specifying how deep the minimax should search is the optional
argument. For human, a string with no spaces is the "username"/nickname of the
human playing. None of the other bots have any argument attached to them.

The *random* bot places a piece randomly, and will give the opponent a random
piece. 

The *novice* bot places their piece in a winning position if it can win
immediately by placing the piece. Otherwise, it will place their piece
randomly. The bot will then give the opponent a random piece which the opponent
cannot immediately win with in the next round, given that such a piece
exists. Otherwise, it will give a random piece.

The *minimax* bot places their piece based on a *d* depth minimax search with
alpha-beta pruning. If the bot doesn't reach every end state, it will perform
the heuristic implemented in `src/snigil/players/minimax.clj`.

The *human* bot is not a bot, but an interface for humans to play against the
different agents.

### Options

To set the number of games (default 1), use `-n` or `--games` to specify the
desired amount of games to play.

To set the verbosity of each game, use `-v` or `--verbosity` to specify the
verbosity of a game. "none" will print nothing, only the summary. "normal" will
print either `1`, `2` or `-` for every game, depending on the outcome. "verbose"
will print every single move both of the players do. Verbose is manually turned
on if a human is in the game.

`--tournament` will set up a tournament mode, where it reads the current board
as a sequence of 16 integers, and then the piece to place. All values are
between -1 and 16. If a board-tile is -1, then it means no piece is placed
there. If the piece to place is -1, then that means you're starting, deciding
what piece the opponent should place out. Output is three integers: two
describing zero-indexed row and column position. The last is the piece the
opponent should place out.

`--help` will print out a shorter summary of this usage-page.

## Example usage

```sh
$ java -jar snigil.jar -n 100 random novice
 # plays 100 rounds random vs. novice
$ java -jar snigil.jar -n 2 human hyPiRion novice
 # plays 2 rounds where a human named "hyPiRion" plays against a novice
$ java -jar -n 100 -v verbose minimax 3 minimax 4
 # verbosily play 100 rounds of minimax with depth 3 and minimax with depth 4
$ java -jar --tournament minimax 4
 # let minimax with depth 4 play a tournament - usually this is used in
 # server-scripts which reroute stdin and stdout.
```

## License

Copyright © 2012 Jean Niklas L'orange

Distributed under the Eclipse Public License, the same as Clojure.

[lein]: http://leiningen.org/ "Leiningen"
