struct student{
    int id ;
    char[10] name ;
}

int x;
int x;
int y;
*int z;
char ch;
float f;
int[10] array;
int[3][3] matrix;

x = 0xf;
x = 0;
c = 0;
y = 10;
ch = '\t';
f = 3.14;

if (x < 5) then
    x = x + 1;
else
    x = x + 2;

x = 0;
y = 0;

while (x < 10) do
    array[x] = x;
    x = x + 1;

array[0] = 0;
matrix[2][1] = 1;
student.id = 1;
x = student.i;
x = student.x;
x = student.id;
x = array[0];
x = x + 1;
x = x * 3;

proc int getsum(int a, int b) {
	return a + b;
}

call getsum(x, 1);