
## Guess Number Game

```
import cstd::*;
import sric::*;

fun main(): Int {
    //init random seed
    srand(currentTimeMillis());

    //random 0..50
    var expected = (rand() / (RAND_MAX as Float32)  * 50) as Int;
    var guess = 0;

    printf("Please input your guess\n");
    while (true) {
        //get input
        var guess = 0;
        scanf("%d", (&guess as raw*Int));

        if (guess > expected) {
            printf("Too big\n");
        }
        else if (guess < expected) {
            printf("Too small\n");
        }
        else {
            printf("You win!\n");
            break;
        }
    }
    return 0;
}
```
