#include <stdint.h>

typedef unsigned long bitset_entry_t;
#define BITS (sizeof(bitset_entry_t) * 8)
typedef struct bitset_t {
    bitset_entry_t data[256/BITS];
} bitset_t;

static inline bitset_t *bitset_init(bitset_t *set)
{
    unsigned i;
    for (i = 0; i < 256 / BITS; i++) {
        set->data[i] = 0;
    }
    return set;
}

static inline void bitset_set(bitset_t *set, unsigned index)
{
    bitset_entry_t mask = ((bitset_entry_t)1) << (index % BITS);
    set->data[index / BITS] |= mask;
}

static inline int bitset_get(bitset_t *set, unsigned index)
{
    bitset_entry_t mask = ((bitset_entry_t)1) << (index % BITS);
    return (set->data[index / BITS] & mask) != 0;
}

#if 0
#include <stdio.h>
int main(int argc, char const* argv[])
{
    bitset_t set;
    bitset_init(&set);
    bitset_set(&set, 2);
    fprintf(stderr, "%d\n", bitset_get(&set, 0));
    fprintf(stderr, "%d\n", bitset_get(&set, 1));
    fprintf(stderr, "%d\n", bitset_get(&set, 2));
    fprintf(stderr, "%d\n", bitset_get(&set, 3));
    return 0;
}
#endif
