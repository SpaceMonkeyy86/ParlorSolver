# ParlorSolver

This is a solver for the parlor logic puzzles in the video game *Blue Prince*. The parlor contains three boxes, each with zero or more statements written on them. One of the boxes contains two gems, one of the game's currencies. There is a wind-up key in the room that you can use to open one box.

The statements on each box can generally be about anything, and each statement could be either true or false. However, the puzzle always follows these rules (written on a sheet of paper on the parlor desk):
- There will always be at least one box which displays only true statements.
- There will always be at least one box which displays only false statements.
- Only one box has a prize within. The other 2 are always empty.

Note that the box that contains the gems does not necessarily have true statements written on it, and boxes with only true statements could be empty. The goal is not to find which statements are true or false (in fact, that is sometimes impossible), simply to find which box contains the gems.

Here is an example puzzle. The correct box to open is the blue box.
- Blue box: "The black box is false."
- White box: "The black box is true."
- Black box: "A false box contains the gems."

For further explanation of the rules of the parlor puzzle, see [this page](https://blueprince.wiki.gg/wiki/Parlor_Game). For the complete list of puzzles used to create this solver, see [here](https://blueprince.wiki.gg/wiki/Parlor_Game/List_of_Parlor_Games).

# Using the solver

Open the project in IntelliJ IDEA and click the big green triangle that says "Run". If you have Java 23 or later installed, you can also download the .jar file and run it directly.

You can input any puzzle found in Blue Prince into the program, and it should find the correct solution. However, the program will not understand arbitrary English sentences that do not have the form of the statements in-game.

When entering multiple statements on a box, put one statement on each line. Once you have entered all statements on a box, input a blank line to move to the next box.

# Strategy

The unknowns in this puzzle are whether each statement is true or false and whether each box is empty or not. For puzzles where each box contains one statement (most of them), this means there are 64 possible cases, though for some puzzles there could be as many as 4096. Each case is either consistent or inconsistent: the gems could be in multiple or no boxes, all boxes could be true or false, or statements may contradict each other. The solver finds which of these cases are consistent, and as long as the puzzle is solvable, each consistent case will have the gems in the same box.

First, the solver parses each statement from natural language into a sentence in first order logic. The model consists of numbers, colors, boxes, statements, and the values true and false. The language contains a constant for each of these, a variable for the truth or falsity of each statement and the presence of gems in each box, and functions for all operations that can be expressed by a statement on a box. Note that the program does not separate formulas into terms, atomic formulas, and well-formed formulas, and does not differentiate between functions, predicates, and connectives. 

The structure of the formulas was made to align with how ideas are expressed in natural language. This means quantifiers work very differently than how they would in regular mathematical logic. Take the phrases "This box", "A box", and "Every box", for example. They each mean different things, but they all represent boxes. Now imagine the sentences "(box) is true.", "(box) is false.", "(box) contains gems.", and "(box) is empty." These each parse to their own formulas, but any kind of box listed previously is valid in them. In mathematical logic, the sentences "This box is true." and "Every box is true." are represented differently; one is just a predicate and the other is a predicate bound by a quantifier. This poorly represents how the English language expresses quantities, however. In this project, quantifiers are a property of the terms themselves, and they do not appear at the connective level.

To facilitate this representation, this project uses "groups". A group is a collection of values that can be used in place of a single value. The statements "A box" and "Every box" would each parse to a group. In fact, they parse to the same group: the set of all boxes. The difference is that one formula has the "at least one" quantifier, and the other has the "all" quantifier. When passed into a predicate, such as "(box) is true", the predicate is evaluated individually for every element in the group, the program counts the number of true and false results, and the quantifier then determines whether the entire expression is true or false. Groups can be substituted for single values anywhere in a formula, causing the evaluation to perform a map, flat map, filter, or quantifier operation where appropriate.

The "brute-force" approach used by the solver works out-of-the-box for almost all puzzles. However, there are some puzzles where this won't work. Some puzzles force you to rely on the assumption that the puzzle is solvable in order to solve the puzzle; they require reasoning such as "If this statement were true, the puzzle would become unsolvable, so this statement cannot be true." To emulate this, the solver uses heuristics as a fallback to eliminate possibly dubious solutions. These heuristics examine the symmetry of statements and boxes. If two boxes have identical wording, it does not make sense for one to be true and the other to be false, since then flipping their truth values would result in a valid solution as well. And if only a single statement proclaims the location of the gems (say "The gems are in the blue box."), then that statement must be true, since otherwise it would be impossible to choose between the other two boxes.

# Reflection

By far the hardest part of this project was parsing each statement from natural language to a formula. Some statements are either very convoluted (e.g. "If you replace the word 'one' in the other two statements with 'both', they will both be false.") or simply mean (e.g. "You will open this box and find it empty."). I expected this project to take under 15 hours, but it took well over 30 hours by the end. I have an intense and bitter hatred for both the English language and this puzzle now. Good thing I don't need to solve it by hand anymore :)
