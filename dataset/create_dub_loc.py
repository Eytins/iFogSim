import csv


def main():
    with open('usersLocation-dubCBD_1.csv', 'r') as input_file, open('usersLocation-dubCBD_1.csv', 'w') as output_file:
        reader = csv.reader(input_file)
        for row in reader:
            # writer.writerow(row)
            # 91.15220384952826,-151.22496428155532
            # 53.338711015192935+37.81349283433532, -6.272593768597314-144.952370512958
            output_file.write(str(float(row[0]) + 91.15220384952826) + ', ' + str(float(row[1]) - 151.22496428155532) + '\n')


if __name__ == '__main__':
    main()
