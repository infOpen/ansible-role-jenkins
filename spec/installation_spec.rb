require 'serverspec'

if ENV['TRAVIS']
    set :backend, :exec
end

describe 'jenkins Ansible role' do

if ['debian', 'ubuntu'].include?(os[:family])
    describe 'Specific Debian and Ubuntu family checks' do

        it 'install role packages' do
            packages = Array[ 'apt' ]

            packages.each do |pkg_name|
                expect(package(pkg_name)).to be_installed
            end
        end

    end
end

end
