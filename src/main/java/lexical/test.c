struct Student {
    int id;
    int name;
} student;

void main() {
    int x;
    char y;
    int z[2];
    int p[2][2];
    int count;
    x = 5;
    y = '\t';
    z[1] = 0;
    p[1][0] = 6;
    x = z[1];
    x = p[1][0];
    if (x < 10) {
        y = '\n';
    } else {
        y = '\r';
    }
    count = 0;
    do {
        count = count + 1;
        x = x - 1;
        swap(x, 2);
    } while (count < 10 && x > 0)
}

void swap(int a, int b) {
    int tmp;
    tmp = a;
    a = b;
    b = tmp;
}