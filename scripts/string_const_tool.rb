#!/usr/bin/ruby -w

#
# String const tool
#
# Puts a block of text into a form java sees as a string constant.
#
# It's best to use this program in conjunction with, another handy tool,
# the "fold" program. 
# Try fold -w 70 -s
# or, in vim, set tw=70 and then press "gq"
#
# Example:
# INPUT:
# Four score and seven years ago our fathers brought forth on this
# continent a new nation, conceived in liberty and dedicated to the
# proposition that all men are created equal
# 
# OUTPUT:
# "Four score and seven years ago our fathers brought forth on this " +
# "continent a new nation, conceived in liberty and dedicated to the " +
# "proposition that all men are created equal"
#

def print_line(line, insert_trailing_space, op)
  if (op == :deconstify) then
    while (line =~ /["+ ]$/)
      line.chop!
    end
    while (line =~ /^[" ]/)
      line = line[1..-1]
    end
    puts line
  elsif (op == :constify) then
    while (line =~ / $/)
      line.chop!
    end
    trailer = insert_trailing_space ? " " : ""
    plus = insert_trailing_space ? " +" : ";"
    puts "\"#{line}#{trailer}\"#{plus}"
  else
    raise "logic error"
  end
end

arg=ARGV[0]
if (arg == "-d") then
  op=:deconstify
elsif (arg == "-c") then
  op=:constify
else
  $stderr.puts "Argument must be either -d or -c"
  exit 1
end

prevline = nil
while((line = STDIN.gets))
  print_line(prevline, true, op) unless (prevline == nil)
  prevline = line.chomp
end
print_line(prevline, false, op) unless (prevline == nil)

exit 0
