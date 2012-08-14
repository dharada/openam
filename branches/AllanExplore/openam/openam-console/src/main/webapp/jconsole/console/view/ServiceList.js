/**
 * Created with IntelliJ IDEA.
 * User: allan
 * Date: 8/9/12
 * Time: 17:05
 * To change this template use File | Settings | File Templates.
 */



Ext.define('AM.view.ServiceList', {
    extend: 'Ext.grid.Panel',
    alias:   'widget.servicelist',


    title:   'Services',
    autoScroll: true,

    initComponent: function() {
        this.columns        = [
            { text: 'Service',  dataIndex: 'name', flex: 1}
        ];
        console.log('Initializing Servicelist');
        this.callParent(arguments);
    }
});
